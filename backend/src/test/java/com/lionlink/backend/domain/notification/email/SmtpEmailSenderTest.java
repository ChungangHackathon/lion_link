package com.lionlink.backend.domain.notification.email;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

class SmtpEmailSenderTest {

	@Test
	void 한글_표시_이름은_헤더에서_인코딩된다() throws Exception {
		MimeMessage message = new JavaMailSenderImpl().createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());

		helper.setFrom(SmtpEmailSender.fromAddress("멋사동네 <no-reply@lionlink.local>"));
		message.saveChanges();

		assertThat(message.getHeader("From", null))
				.contains("=?UTF-8?")
				.contains("no-reply@lionlink.local")
				.doesNotContain("멋사동네");
	}

	@Test
	void 표시_이름이_없으면_주소만_쓴다() throws Exception {
		assertThat(SmtpEmailSender.fromAddress("no-reply@lionlink.local").getAddress())
				.isEqualTo("no-reply@lionlink.local");
	}
}
