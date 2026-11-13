package com.lionlink.backend.domain.coffeechat.service;

import com.lionlink.backend.domain.alumni.exception.AlumniErrorCode;
import com.lionlink.backend.domain.chat.service.CoffeeChatRoomService;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatAcceptRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatCancelRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatCreateRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatRejectRequest;
import com.lionlink.backend.domain.coffeechat.dto.request.CoffeeChatScheduleRequest;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatCreateResponse;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatDetailResponse;
import com.lionlink.backend.domain.coffeechat.dto.response.CoffeeChatRole;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMember;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMemberRole;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import com.lionlink.backend.domain.coffeechat.entity.MeetingMode;
import com.lionlink.backend.domain.coffeechat.event.CoffeeChatNotificationEvent;
import com.lionlink.backend.domain.coffeechat.event.CoffeeChatNotificationType;
import com.lionlink.backend.domain.coffeechat.exception.CoffeeChatErrorCode;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeChatMemberRepository;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeChatRepository;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeSettingRepository;
import com.lionlink.backend.domain.community.exception.CommunityErrorCode;
import com.lionlink.backend.domain.community.post.entity.Post;
import com.lionlink.backend.domain.community.post.entity.PostCategory;
import com.lionlink.backend.domain.community.post.repository.PostRepository;
import com.lionlink.backend.domain.community.recruit.entity.CoffeeChatRecruit;
import com.lionlink.backend.domain.community.recruit.entity.RecruitMember;
import com.lionlink.backend.domain.community.recruit.entity.RecruitStatus;
import com.lionlink.backend.domain.community.recruit.repository.CoffeeChatRecruitRepository;
import com.lionlink.backend.domain.community.recruit.repository.RecruitMemberRepository;
import com.lionlink.backend.domain.profile.CoffeeAvailability;
import com.lionlink.backend.domain.profile.CoffeeAvailabilityReader;
import com.lionlink.backend.domain.profile.UserSummary;
import com.lionlink.backend.domain.profile.UserSummaryReader;
import com.lionlink.backend.domain.user.entity.UserStatus;
import com.lionlink.backend.domain.user.repository.UserRepository;
import com.lionlink.backend.global.error.BusinessException;
import com.lionlink.backend.global.error.CommonErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class CoffeeChatService {

	private static final String ETC_TOPIC = "ETC";
	private static final int EXPIRE_DAYS = 14;
	private static final int AUTO_COMPLETE_AFTER_MEETING_DAYS = 7;
	private static final int AUTO_COMPLETE_AFTER_ACCEPT_DAYS = 30;
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("M월 d일");
	private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("M월 d일 HH:mm");

	private final CoffeeChatRepository coffeeChatRepository;
	private final CoffeeChatMemberRepository coffeeChatMemberRepository;
	private final CoffeeSettingRepository coffeeSettingRepository;
	private final CoffeeChatQueryService coffeeChatQueryService;
	private final CoffeeChatCooldownPolicy cooldownPolicy;
	private final CoffeeChatRoomService coffeeChatRoomService;
	private final CoffeeAvailabilityReader coffeeAvailabilityReader;
	private final UserRepository userRepository;
	private final UserSummaryReader userSummaryReader;
	private final PostRepository postRepository;
	private final CoffeeChatRecruitRepository coffeeChatRecruitRepository;
	private final RecruitMemberRepository recruitMemberRepository;
	private final ApplicationEventPublisher eventPublisher;

	public CoffeeChatCreateResponse create(Long userId, CoffeeChatCreateRequest request) {
		LocalDateTime now = LocalDateTime.now();
		CoffeeChatRecruit recruit = request.fromRecruitId() == null ? null : findOpenRecruit(userId, request.fromRecruitId());
		Long receiverId = recruit != null ? recruit.getSeniorId() : request.receiverId();
		if (receiverId == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR, "커피챗을 받을 사자를 골라 주세요.");
		}
		if (receiverId.equals(userId)) {
			throw new BusinessException(CoffeeChatErrorCode.CANNOT_REQUEST_SELF);
		}
		if (!userRepository.existsByIdAndStatusAndDeletedAtIsNull(receiverId, UserStatus.APPROVED)) {
			throw new BusinessException(AlumniErrorCode.USER_NOT_FOUND);
		}
		List<Long> memberIds = recruit != null ? recruitMemberIds(userId, recruit) : validateMembers(userId, receiverId, request.memberIds());

		CoffeeAvailability availability = coffeeAvailabilityReader.getAvailability(receiverId);
		if (!availability.available()) {
			throw new BusinessException(CoffeeChatErrorCode.NOT_ACCEPTING);
		}
		if (!memberIds.isEmpty() && !availability.acceptGroup()) {
			throw new BusinessException(CoffeeChatErrorCode.GROUP_COFFEE_CHAT_NOT_ALLOWED);
		}
		String topicCustom = validateTopic(receiverId, request.topic(), request.topicCustom());
		List<Long> applicantIds = Stream.concat(Stream.of(userId), memberIds.stream()).toList();
		if (coffeeChatRepository.existsOpenChatWithAny(receiverId, applicantIds)) {
			throw new BusinessException(CoffeeChatErrorCode.OPEN_COFFEE_CHAT_EXISTS);
		}
		cooldownPolicy.availableFrom(userId, receiverId, now.toLocalDate()).ifPresent(date -> {
			throw new BusinessException(CoffeeChatErrorCode.COOLDOWN, date.format(DAY) + "부터 다시 신청할 수 있어요.");
		});

		CoffeeChat chat = save(CoffeeChat.builder()
				.requesterId(userId)
				.receiverId(receiverId)
				.topic(request.topic().trim())
				.topicCustom(topicCustom)
				.reason(request.reason().trim())
				.questions(Objects.requireNonNullElse(request.questions(), List.<String>of()).stream().map(String::trim).toList())
				.preferredMode(request.preferredMode())
				.scheduleNote(StringUtils.hasText(request.scheduleNote()) ? request.scheduleNote().trim() : null)
				.expiresAt(now.plusDays(EXPIRE_DAYS))
				.build());
		if (!memberIds.isEmpty()) {
			List<CoffeeChatMember> members = new ArrayList<>();
			members.add(CoffeeChatMember.builder().coffeeChat(chat).userId(userId).role(CoffeeChatMemberRole.REPRESENTATIVE).build());
			memberIds.forEach(memberId -> members.add(
					CoffeeChatMember.builder().coffeeChat(chat).userId(memberId).role(CoffeeChatMemberRole.MEMBER).build()));
			coffeeChatMemberRepository.saveAll(members);
		}
		if (recruit != null) {
			recruit.apply(chat.getId());
		}

		String requesterName = name(userId);
		String message = memberIds.isEmpty()
				? requesterName + "님이 커피챗을 신청했어요."
				: requesterName + "님 외 " + memberIds.size() + "명이 커피챗을 신청했어요.";
		notify(List.of(receiverId), CoffeeChatNotificationType.COFFEE_CHAT_REQUESTED, chat.getId(), message);
		return CoffeeChatCreateResponse.from(chat);
	}

	public CoffeeChatDetailResponse accept(Long userId, Long coffeeChatId, CoffeeChatAcceptRequest request) {
		LocalDateTime now = LocalDateTime.now();
		CoffeeChat chat = findParticipating(userId, coffeeChatId);
		requireRole(chat.isReceiver(userId));
		requirePending(chat, now);

		chat.accept(request.proposedSlots().stream().map(LocalDateTime::toString).toList(), trimToNull(request.responseMessage()), now);
		List<Long> memberIds = activeMemberIds(chat);
		coffeeChatRoomService.open(chat, memberIds, now);
		coffeeChatRoomService.postSystemMessage(chat.getId(), "커피챗이 수락됐어요. 채팅으로 일정을 정해보세요.", now);
		notify(applicants(chat, memberIds), CoffeeChatNotificationType.COFFEE_CHAT_ACCEPTED, chat.getId(),
				name(chat.getReceiverId()) + "님이 커피챗을 수락했어요. 채팅방에서 일정을 정해보세요.");
		return coffeeChatQueryService.toDetail(userId, chat);
	}

	public CoffeeChatDetailResponse reject(Long userId, Long coffeeChatId, CoffeeChatRejectRequest request) {
		LocalDateTime now = LocalDateTime.now();
		CoffeeChat chat = findParticipating(userId, coffeeChatId);
		requireRole(chat.isReceiver(userId));
		requirePending(chat, now);

		chat.reject(request.rejectReason(), trimToNull(request.responseMessage()), now);
		notify(applicants(chat, activeMemberIds(chat)), CoffeeChatNotificationType.COFFEE_CHAT_REJECTED, chat.getId(),
				name(chat.getReceiverId()) + "님이 이번 커피챗은 어렵다고 했어요.");
		return coffeeChatQueryService.toDetail(userId, chat);
	}

	public CoffeeChatDetailResponse withdraw(Long userId, Long coffeeChatId) {
		LocalDateTime now = LocalDateTime.now();
		CoffeeChat chat = findParticipating(userId, coffeeChatId);
		requireRole(chat.isRequester(userId));
		requirePending(chat, now);

		chat.cancel(userId, null, now);
		return coffeeChatQueryService.toDetail(userId, chat);
	}

	public CoffeeChatDetailResponse schedule(Long userId, Long coffeeChatId, CoffeeChatScheduleRequest request) {
		LocalDateTime now = LocalDateTime.now();
		CoffeeChat chat = findParticipating(userId, coffeeChatId);
		requireRole(chat.isReceiver(userId) || chat.isRequester(userId));
		requireStatus(chat, CoffeeChatStatus.ACCEPTED, CoffeeChatStatus.SCHEDULED);

		boolean changed = chat.getStatus() == CoffeeChatStatus.SCHEDULED;
		boolean offline = request.meetingMode() == MeetingMode.OFFLINE;
		chat.schedule(request.scheduledAt(), request.meetingMode(),
				offline ? request.meetingPlace().trim() : null,
				offline ? null : trimToNull(request.meetingLink()));
		String when = request.scheduledAt().format(DAY_TIME);
		coffeeChatRoomService.postSystemMessage(chat.getId(), (changed ? "일정이 바뀌었어요. " : "일정이 확정됐어요. ") + when
				+ " · " + (offline ? chat.getMeetingPlace() : "온라인"), now);
		notify(othersThan(userId, chat), CoffeeChatNotificationType.COFFEE_CHAT_SCHEDULED, chat.getId(),
				(changed ? "커피챗 일정이 바뀌었어요. " : "커피챗 일정이 정해졌어요. ") + when);
		return coffeeChatQueryService.toDetail(userId, chat);
	}

	public CoffeeChatDetailResponse cancel(Long userId, Long coffeeChatId, CoffeeChatCancelRequest request) {
		LocalDateTime now = LocalDateTime.now();
		CoffeeChat chat = findParticipating(userId, coffeeChatId);
		requireRole(chat.isReceiver(userId) || chat.isRequester(userId));
		requireStatus(chat, CoffeeChatStatus.ACCEPTED, CoffeeChatStatus.SCHEDULED);

		chat.cancel(userId, request.cancelReason().trim(), now);
		coffeeChatRoomService.postSystemMessage(chat.getId(), "커피챗이 취소됐어요.", now);
		notify(othersThan(userId, chat), CoffeeChatNotificationType.COFFEE_CHAT_CANCELED, chat.getId(),
				name(userId) + "님이 커피챗을 취소했어요.");
		return coffeeChatQueryService.toDetail(userId, chat);
	}

	public CoffeeChatDetailResponse complete(Long userId, Long coffeeChatId) {
		LocalDateTime now = LocalDateTime.now();
		CoffeeChat chat = findParticipating(userId, coffeeChatId);
		requireRole(chat.isReceiver(userId) || chat.isRequester(userId));
		requireStatus(chat, CoffeeChatStatus.SCHEDULED);
		if (chat.getScheduledAt().isAfter(now)) {
			throw new BusinessException(CoffeeChatErrorCode.MEETING_NOT_PASSED);
		}

		chat.complete(now);
		coffeeChatRoomService.postSystemMessage(chat.getId(), "커피챗을 마쳤어요.", now);
		return coffeeChatQueryService.toDetail(userId, chat);
	}

	public void leave(Long userId, Long coffeeChatId) {
		LocalDateTime now = LocalDateTime.now();
		CoffeeChat chat = findParticipating(userId, coffeeChatId);
		CoffeeChatMember member = coffeeChatMemberRepository.findByCoffeeChatIdAndUserId(coffeeChatId, userId)
				.filter(CoffeeChatMember::isActiveMember)
				.orElseThrow(() -> new BusinessException(CoffeeChatErrorCode.FORBIDDEN_ACTION));
		if (!chat.isOpen()) {
			throw new BusinessException(CoffeeChatErrorCode.INVALID_STATUS);
		}
		member.leave(now);
		coffeeChatRoomService.leave(coffeeChatId, userId, now);
	}

	public int expireOverdue(LocalDateTime now) {
		int count = 0;
		for (Long id : coffeeChatRepository.findIdsToExpire(CoffeeChatStatus.PENDING, now)) {
			CoffeeChat chat = coffeeChatRepository.findByIdForUpdate(id).orElseThrow();
			if (!chat.isExpired(now)) {
				continue;
			}
			chat.expire();
			notify(applicants(chat, activeMemberIds(chat)), CoffeeChatNotificationType.COFFEE_CHAT_EXPIRED, chat.getId(),
					name(chat.getReceiverId()) + "님에게 보낸 커피챗 신청이 만료됐어요.");
			count++;
		}
		return count;
	}

	public int completeOverdue(LocalDateTime now) {
		int count = 0;
		for (Long id : coffeeChatRepository.findIdsScheduledBefore(CoffeeChatStatus.SCHEDULED,
				now.minusDays(AUTO_COMPLETE_AFTER_MEETING_DAYS))) {
			count += completeIf(id, CoffeeChatStatus.SCHEDULED, now);
		}
		for (Long id : coffeeChatRepository.findIdsAcceptedBefore(CoffeeChatStatus.ACCEPTED,
				now.minusDays(AUTO_COMPLETE_AFTER_ACCEPT_DAYS))) {
			count += completeIf(id, CoffeeChatStatus.ACCEPTED, now);
		}
		return count;
	}

	private int completeIf(Long id, CoffeeChatStatus expected, LocalDateTime now) {
		CoffeeChat chat = coffeeChatRepository.findByIdForUpdate(id).orElseThrow();
		if (chat.getStatus() != expected) {
			return 0;
		}
		chat.complete(now);
		return 1;
	}

	private CoffeeChat save(CoffeeChat chat) {
		try {
			return coffeeChatRepository.saveAndFlush(chat);
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(CoffeeChatErrorCode.OPEN_COFFEE_CHAT_EXISTS);
		}
	}

	private CoffeeChatRecruit findOpenRecruit(Long userId, Long postId) {
		Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
				.filter(found -> found.getCategory() == PostCategory.RECRUIT)
				.orElseThrow(() -> new BusinessException(CommunityErrorCode.RECRUIT_NOT_FOUND));
		CoffeeChatRecruit recruit = coffeeChatRecruitRepository.findByIdForUpdate(postId)
				.orElseThrow(() -> new BusinessException(CommunityErrorCode.RECRUIT_NOT_FOUND));
		if (!post.isWrittenBy(userId)) {
			throw new BusinessException(CommunityErrorCode.NOT_OWNER, "모집글 작성자만 커피챗을 신청할 수 있어요.");
		}
		if (recruit.statusOn(LocalDate.now()) != RecruitStatus.OPEN) {
			throw new BusinessException(CommunityErrorCode.RECRUIT_CLOSED);
		}
		return recruit;
	}

	private List<Long> recruitMemberIds(Long userId, CoffeeChatRecruit recruit) {
		List<Long> joined = recruitMemberRepository.findByRecruitPostIdOrderByJoinedAtAscUserIdAsc(recruit.getPostId()).stream()
				.map(RecruitMember::getUserId)
				.filter(memberId -> !memberId.equals(userId))
				.toList();
		Set<Long> active = joined.isEmpty() ? Set.of() : userRepository.findIdsByStatus(joined, UserStatus.APPROVED);
		List<Long> memberIds = joined.stream().filter(active::contains).toList();
		if (memberIds.isEmpty()) {
			throw new BusinessException(CoffeeChatErrorCode.INVALID_MEMBERS, "참여자가 작성자 외 1명 이상일 때 신청할 수 있어요.");
		}
		return memberIds;
	}

	private List<Long> validateMembers(Long userId, Long receiverId, List<Long> requested) {
		List<Long> memberIds = Objects.requireNonNullElse(requested, List.of());
		List<Long> distinct = memberIds.stream().distinct().toList();
		if (distinct.size() != memberIds.size() || distinct.contains(userId) || distinct.contains(receiverId)) {
			throw new BusinessException(CoffeeChatErrorCode.INVALID_MEMBERS);
		}
		if (!distinct.isEmpty()
				&& userRepository.countByIdInAndStatusAndDeletedAtIsNull(distinct, UserStatus.APPROVED) != distinct.size()) {
			throw new BusinessException(CoffeeChatErrorCode.INVALID_MEMBERS);
		}
		return distinct;
	}

	private String validateTopic(Long receiverId, String topic, String topicCustom) {
		String code = topic.trim();
		if (ETC_TOPIC.equals(code)) {
			if (!StringUtils.hasText(topicCustom)) {
				throw new BusinessException(CoffeeChatErrorCode.INVALID_TOPIC, "주제를 직접 입력해 주세요.");
			}
			return topicCustom.trim();
		}
		List<String> topics = coffeeSettingRepository.findById(receiverId)
				.map(CoffeeSetting::getTopics)
				.orElse(List.of());
		if (topics == null || !topics.contains(code)) {
			throw new BusinessException(CoffeeChatErrorCode.INVALID_TOPIC);
		}
		return null;
	}

	private CoffeeChat findParticipating(Long userId, Long coffeeChatId) {
		CoffeeChat chat = coffeeChatRepository.findByIdForUpdate(coffeeChatId)
				.orElseThrow(() -> new BusinessException(CoffeeChatErrorCode.COFFEE_CHAT_NOT_FOUND));
		if (CoffeeChatQueryService.roleOf(chat, userId, activeMemberIds(chat)) == null) {
			throw new BusinessException(CoffeeChatErrorCode.COFFEE_CHAT_NOT_FOUND);
		}
		return chat;
	}

	private void requireRole(boolean allowed) {
		if (!allowed) {
			throw new BusinessException(CoffeeChatErrorCode.FORBIDDEN_ACTION);
		}
	}

	private void requirePending(CoffeeChat chat, LocalDateTime now) {
		requireStatus(chat, CoffeeChatStatus.PENDING);
		if (chat.isExpired(now)) {
			throw new BusinessException(CoffeeChatErrorCode.INVALID_STATUS, "답할 수 있는 기간이 지나 만료된 신청이에요.");
		}
	}

	private void requireStatus(CoffeeChat chat, CoffeeChatStatus... allowed) {
		if (!List.of(allowed).contains(chat.getStatus())) {
			throw new BusinessException(CoffeeChatErrorCode.INVALID_STATUS);
		}
	}

	private List<Long> activeMemberIds(CoffeeChat chat) {
		return coffeeChatQueryService.activeMemberIds(List.of(chat)).getOrDefault(chat.getId(), List.of());
	}

	private List<Long> applicants(CoffeeChat chat, List<Long> memberIds) {
		return Stream.concat(Stream.of(chat.getRequesterId()), memberIds.stream()).toList();
	}

	private List<Long> othersThan(Long userId, CoffeeChat chat) {
		return Stream.concat(Stream.of(chat.getReceiverId()), applicants(chat, activeMemberIds(chat)).stream())
				.filter(id -> !id.equals(userId))
				.toList();
	}

	private String name(Long userId) {
		return userSummaryReader.getSummary(userId)
				.filter(summary -> !summary.deleted())
				.map(UserSummary::name)
				.orElse("동문");
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private void notify(List<Long> userIds, CoffeeChatNotificationType type, Long coffeeChatId, String message) {
		if (!userIds.isEmpty()) {
			eventPublisher.publishEvent(new CoffeeChatNotificationEvent(userIds, type, coffeeChatId, message));
		}
	}
}
