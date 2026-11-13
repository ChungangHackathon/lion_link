package com.lionlink.backend.domain.notification.email;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeChat;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMemberRole;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatStatus;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeChatMemberRepository;
import com.lionlink.backend.domain.coffeechat.repository.CoffeeChatRepository;
import com.lionlink.backend.domain.notification.config.EmailProperties;
import com.lionlink.backend.domain.notification.entity.EmailType;
import com.lionlink.backend.domain.user.entity.User;
import com.lionlink.backend.domain.user.repository.UserRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

/**
 * 메일 제목과 본문을 만든다. 보내는 시점의 데이터로 만들고,
 * 대상이 사라졌거나 이미 처리돼 보낼 필요가 없으면 빈 값을 돌려준다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailContentFactory {

	private static final String PREFIX = "[LION:LINK] ";
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("M/d");
	private static final Map<String, String> TOPICS = Map.of(
			"JOB_PREP", "취업 준비",
			"CAREER", "커리어/이직",
			"STUDY", "학업/전공",
			"PROJECT_DEV", "프로젝트/개발",
			"STARTUP", "창업",
			"LION_ACTIVITY", "멋사 활동"
	);

	private final CoffeeChatRepository coffeeChatRepository;
	private final CoffeeChatMemberRepository coffeeChatMemberRepository;
	private final UserRepository userRepository;
	private final EmailProperties properties;

	public Optional<EmailContent> render(EmailType type, Long targetId, String unsubscribeUrl) {
		return switch (type) {
			case COFFEE_CHAT_REQUEST -> coffeeChatRequest(targetId, unsubscribeUrl);
			case MESSAGE_REQUEST -> Optional.of(messageRequest(unsubscribeUrl));
			case SIGNUP_APPROVED -> signupApproved(targetId, unsubscribeUrl);
		};
	}

	/** N-02 알림 메일 템플릿. 대기 중인 신청에만 보낸다. */
	private Optional<EmailContent> coffeeChatRequest(Long coffeeChatId, String unsubscribeUrl) {
		Optional<CoffeeChat> found = coffeeChatRepository.findById(coffeeChatId)
				.filter(chat -> chat.getStatus() == CoffeeChatStatus.PENDING);
		if (found.isEmpty()) {
			return Optional.empty();
		}
		CoffeeChat chat = found.get();
		Optional<User> requester = userRepository.findById(chat.getRequesterId());
		if (requester.isEmpty()) {
			return Optional.empty();
		}
		long memberCount = coffeeChatMemberRepository.findByCoffeeChatIdIn(List.of(coffeeChatId)).stream()
				.filter(member -> member.getRole() == CoffeeChatMemberRole.MEMBER && member.getLeftAt() == null)
				.count();
		String who = requester.get().getName() + "(" + requester.get().getGeneration() + "기)님"
				+ (memberCount > 0 ? " 외 " + memberCount + "명" : "");
		String subject = PREFIX + who + "이 커피챗을 신청했어요";
		String topic = chat.getTopicCustom() != null ? chat.getTopicCustom() : TOPICS.getOrDefault(chat.getTopic(), chat.getTopic());

		String body = paragraph("<b>" + escape(who) + "</b>이 커피챗을 신청했어요. 주제: <b>" + escape(topic) + "</b>")
				+ note(chat.getExpiresAt().format(DAY) + "까지 답하지 않으면 신청이 자동으로 만료돼요.")
				+ button("확인하러 가기", link("/coffee/" + coffeeChatId));
		return Optional.of(new EmailContent(subject, layout(body, "커피챗 신청 메일", unsubscribeUrl)));
	}

	/** 메시지 요청 내용은 채팅에서 정해지므로 지금은 요청함으로 안내만 한다. */
	private EmailContent messageRequest(String unsubscribeUrl) {
		String body = paragraph("새 메시지 요청이 왔어요.")
				+ note("요청함에서 메시지를 확인하고 수락하거나 거절할 수 있어요.")
				+ button("요청함 열기", link("/chat/requests"));
		return new EmailContent(PREFIX + "새 메시지 요청이 왔어요", layout(body, "메시지 요청 메일", unsubscribeUrl));
	}

	private Optional<EmailContent> signupApproved(Long userId, String unsubscribeUrl) {
		return userRepository.findById(userId).map(user -> {
			String body = paragraph("<b>" + escape(user.getName()) + "</b>님, 가입이 승인됐어요.")
					+ note("이제 동문 찾기, 커피챗, 채팅, 커뮤니티를 모두 이용할 수 있어요.")
					+ button("LION:LINK 시작하기", link("/home"));
			return new EmailContent(PREFIX + "가입이 승인됐어요", layout(body, "가입 승인 메일", unsubscribeUrl));
		});
	}

	private String layout(String body, String kind, String unsubscribeUrl) {
		return """
				<div style="font-family:sans-serif;color:#111;max-width:560px;margin:0 auto;padding:24px">
				<p style="font-size:20px;font-weight:700;color:#ff7710;margin:0 0 16px">LION:LINK</p>
				%s
				<p style="margin-top:32px;font-size:12px;color:#999">
				더 이상 받지 않으려면 <a href="%s" style="color:#999">%s 그만 받기</a>를 눌러주세요. 마이페이지 &gt; 알림 설정에서 다시 켤 수 있어요.
				</p>
				</div>
				""".formatted(body, escape(unsubscribeUrl), kind);
	}

	private String paragraph(String html) {
		return "<p style=\"font-size:15px;line-height:24px;margin:0\">" + html + "</p>";
	}

	private String note(String text) {
		return "<p style=\"font-size:13px;color:#666;margin:4px 0 0\">" + escape(text) + "</p>";
	}

	private String button(String label, String url) {
		return "<p style=\"margin-top:16px\"><a href=\"" + escape(url) + "\" style=\"display:inline-block;padding:10px 16px;"
				+ "background:#ff7710;color:#fff;border-radius:8px;text-decoration:none\">" + escape(label) + "</a></p>";
	}

	private String link(String path) {
		return properties.linkBaseUrl() + path;
	}

	private static String escape(String text) {
		return HtmlUtils.htmlEscape(text == null ? "" : text);
	}
}
