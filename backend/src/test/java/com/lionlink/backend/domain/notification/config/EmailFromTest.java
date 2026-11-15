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

	@Test
	void 보내는_사람_주소로_메일을_만들_수_있다() throws Exception {
		MimeMessage message = new JavaMailSenderImpl().createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());

		assertThatCode(() -> helper.setFrom(emailProperties.from())).doesNotThrowAnyException();
	}
}
