package com.lionlink.backend.domain.notification.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

	private final JavaMailSender mailSender;
	private final String from;

	@Override
	public void send(String to, String subject, String html) {
		MimeMessage message = mailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
			helper.setFrom(fromAddress(from));
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(html, true);
		} catch (MessagingException | UnsupportedEncodingException e) {
			throw new MailPreparationException(e);
		}
		mailSender.send(message);
	}

	static InternetAddress fromAddress(String from) throws MessagingException, UnsupportedEncodingException {
		InternetAddress address = new InternetAddress(from);
		if (address.getPersonal() != null) {
			address.setPersonal(address.getPersonal(), StandardCharsets.UTF_8.name());
		}
		return address;
	}
}
