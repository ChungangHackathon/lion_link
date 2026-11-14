package com.lionlink.backend.domain.notification.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

@SpringBootTest
class EmailFromTest {

	@Autowired
	private EmailProperties emailProperties;

	/**
	 * 보내는 사람 주소는 메일을 만들 때 헤더로 해석된다.
	 * 표시 이름에 콜론이 있는데 따옴표가 없으면 여기서 터지고, 메일 서버에 닿지도 못한다.
	 */
	@Test
	void 보내는_사람_주소로_메일을_만들_수_있다() throws Exception {
		MimeMessage message = new JavaMailSenderImpl().createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());

		assertThatCode(() -> helper.setFrom(emailProperties.from())).doesNotThrowAnyException();
	}
}
