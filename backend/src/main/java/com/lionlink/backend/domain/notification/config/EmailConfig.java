package com.lionlink.backend.domain.notification.config;

import com.lionlink.backend.domain.notification.email.EmailSender;
import com.lionlink.backend.domain.notification.email.LoggingEmailSender;
import com.lionlink.backend.domain.notification.email.SmtpEmailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfig {

	/** app.email.enabled가 true이고 SMTP가 설정돼 있을 때만 실제로 보낸다. 로컬과 테스트는 로그만 남긴다. */
	@Bean
	public EmailSender emailSender(EmailProperties properties, ObjectProvider<JavaMailSender> javaMailSender) {
		JavaMailSender mailSender = javaMailSender.getIfAvailable();
		if (properties.enabled() && mailSender != null) {
			return new SmtpEmailSender(mailSender, properties.from());
		}
		return new LoggingEmailSender();
	}
}
