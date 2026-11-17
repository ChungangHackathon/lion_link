package com.lionlink.backend.domain.coffeechat.service;

import com.lionlink.backend.domain.chat.entity.ChatRoom;
import com.lionlink.backend.domain.chat.repository.ChatRoomRepository;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatBox;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatFilter;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatAction;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatDetailResponse;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatListResponse;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatRole;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatSummaryResponse;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatUserResponse;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMember;
import com.lionlink.backend.domain.coffeechat.exception.CoffeeChatErrorCode;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeChatMemberRepository;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeChatQueryRepository;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeChatRepository;
import com.lionlink.backend.domain.community.recruit.entity.CoffeeChatRecruit;
import com.lionlink.backend.domain.community.recruit.repository.CoffeeChatRecruitRepository;
import com.lionlink.backend.domain.profile.UserSummary;
import com.lionlink.backend.domain.profile.UserSummaryReader;
import com.lionlink.backend.global.error.BusinessException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoffeeChatQueryService {

	private final CoffeeChatRepository coffeeChatRepository;
	private final CoffeeChatMemberRepository coffeeChatMemberRepository;
	private final CoffeeChatQueryRepository coffeeChatQueryRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final CoffeeChatRecruitRepository coffeeChatRecruitRepository;
	private final UserSummaryReader userSummaryReader;

	public CoffeeChatListResponse getList(Long userId, CoffeeChatBox box, CoffeeChatFilter filter, Pageable pageable) {
		Slice<CoffeeChat> chats = coffeeChatQueryRepository.findMine(userId, box, filter, pageable);
		Map<Long, List<Long>> membersByChat = activeMemberIds(chats.getContent());
		Set<Long> userIds = new HashSet<>();
		chats.forEach(chat -> {
			userIds.add(chat.getRequesterId());
			userIds.add(chat.getReceiverId());
			userIds.addAll(membersByChat.getOrDefault(chat.getId(), List.of()));
		});
		Map<Long, UserSummary> summaries = userSummaryReader.getSummaries(userIds);

		List<CoffeeChatSummaryResponse> items = chats.getContent().stream()
				.map(chat -> new CoffeeChatSummaryResponse(
						chat.getId(),
						chat.getStatus(),
						roleOf(chat, userId, membersByChat.getOrDefault(chat.getId(), List.of())),
						user(summaries, box == CoffeeChatBox.RECEIVED ? chat.getRequesterId() : chat.getReceiverId()),
						users(summaries, membersByChat.getOrDefault(chat.getId(), List.of())),
						chat.getTopic(),
						chat.getTopicCustom(),
						chat.getReason(),
						chat.getCreatedAt(),
						chat.getExpiresAt(),
						chat.getScheduledAt()
				))
				.toList();
		return new CoffeeChatListResponse(items, chats.getNumber(), chats.getSize(), chats.hasNext(),
				coffeeChatQueryRepository.countPendingReceived(userId), coffeeChatQueryRepository.countActive(userId));
	}

	public CoffeeChatDetailResponse getDetail(Long userId, Long coffeeChatId) {
		CoffeeChat chat = coffeeChatRepository.findById(coffeeChatId)
				.orElseThrow(() -> new BusinessException(CoffeeChatErrorCode.COFFEE_CHAT_NOT_FOUND));
		return toDetail(userId, chat);
	}

	public CoffeeChatDetailResponse toDetail(Long userId, CoffeeChat chat) {
		List<Long> memberIds = activeMemberIds(List.of(chat)).getOrDefault(chat.getId(), List.of());
		CoffeeChatRole role = roleOf(chat, userId, memberIds);
		if (role == null) {
			throw new BusinessException(CoffeeChatErrorCode.COFFEE_CHAT_NOT_FOUND);
		}
		Set<Long> userIds = new HashSet<>(memberIds);
		userIds.add(chat.getRequesterId());
		userIds.add(chat.getReceiverId());
		Map<Long, UserSummary> summaries = userSummaryReader.getSummaries(userIds);

		return new CoffeeChatDetailResponse(
				chat.getId(),
				chat.getStatus(),
				role,
				user(summaries, chat.getReceiverId()),
				user(summaries, chat.getRequesterId()),
				users(summaries, memberIds),
				chat.getTopic(),
				chat.getTopicCustom(),
				chat.getReason(),
				chat.getQuestions(),
				chat.getPreferredMode(),
				chat.getScheduleNote(),
				chat.getProposedSlots() == null ? List.of() : chat.getProposedSlots().stream().map(LocalDateTime::parse).toList(),
				chat.getResponseMessage(),
				chat.getRejectReason(),
				chat.getMeetingMode(),
				chat.getScheduledAt(),
				chat.getMeetingPlace(),
				chat.getMeetingLink(),
				chat.getCancelReason(),
				chat.getCanceledBy(),
				chatRoomRepository.findByCoffeeChatId(chat.getId()).map(ChatRoom::getId).orElse(null),
				coffeeChatRecruitRepository.findByCoffeeChatId(chat.getId()).map(CoffeeChatRecruit::getPostId).orElse(null),
				chat.getCreatedAt(),
				chat.getExpiresAt(),
				chat.getAcceptedAt(),
				chat.getRejectedAt(),
				chat.getCanceledAt(),
				chat.getCompletedAt(),
				actions(chat, role, LocalDateTime.now())
		);
	}

	public Map<Long, List<Long>> activeMemberIds(List<CoffeeChat> chats) {
		if (chats.isEmpty()) {
			return Map.of();
		}
		return coffeeChatMemberRepository.findByCoffeeChatIdIn(chats.stream().map(CoffeeChat::getId).toList()).stream()
				.filter(CoffeeChatMember::isActiveMember)
				.collect(Collectors.groupingBy(member -> member.getCoffeeChat().getId(),
						Collectors.mapping(CoffeeChatMember::getUserId, Collectors.toList())));
	}

	static CoffeeChatRole roleOf(CoffeeChat chat, Long userId, List<Long> activeMemberIds) {
		if (chat.isReceiver(userId)) {
			return CoffeeChatRole.RECEIVER;
		}
		if (chat.isRequester(userId)) {
			return CoffeeChatRole.REQUESTER;
		}
		return activeMemberIds.contains(userId) ? CoffeeChatRole.MEMBER : null;
	}

	static List<CoffeeChatAction> actions(CoffeeChat chat, CoffeeChatRole role, LocalDateTime now) {
		boolean host = role == CoffeeChatRole.RECEIVER || role == CoffeeChatRole.REQUESTER;
		return switch (chat.getStatus()) {
			case PENDING -> {
				if (chat.isExpired(now)) {
					yield List.of();
				}
				yield switch (role) {
					case RECEIVER -> List.of(CoffeeChatAction.ACCEPT, CoffeeChatAction.REJECT);
					case REQUESTER -> List.of(CoffeeChatAction.WITHDRAW);
					case MEMBER -> List.of(CoffeeChatAction.LEAVE);
				};
			}
			case ACCEPTED -> host
					? List.of(CoffeeChatAction.SCHEDULE, CoffeeChatAction.CANCEL)
					: List.of(CoffeeChatAction.LEAVE);
			case SCHEDULED -> {
				if (!host) {
					yield List.of(CoffeeChatAction.LEAVE);
				}
				yield chat.getScheduledAt().isAfter(now)
						? List.of(CoffeeChatAction.SCHEDULE, CoffeeChatAction.CANCEL)
						: List.of(CoffeeChatAction.SCHEDULE, CoffeeChatAction.CANCEL, CoffeeChatAction.COMPLETE);
			}
			default -> List.of();
		};
	}

	private CoffeeChatUserResponse user(Map<Long, UserSummary> summaries, Long userId) {
		return CoffeeChatUserResponse.from(summaries.getOrDefault(userId, UserSummary.withdrawn(userId)));
	}

	private List<CoffeeChatUserResponse> users(Map<Long, UserSummary> summaries, List<Long> userIds) {
		return userIds.stream().map(userId -> user(summaries, userId)).toList();
	}
}
