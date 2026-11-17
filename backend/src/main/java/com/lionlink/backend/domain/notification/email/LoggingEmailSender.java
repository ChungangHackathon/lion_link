package com.lionlink.backend.domain.notification.email;

import lombok.extern.slf4j.Slf4j;

/** 메일을 끈 환경(로컬, 테스트)에서 실제로 보내지 않고 제목만 로그로 남긴다. 본문에는 수신 거부 토큰이 있어 남기지 않는다. */
@Slf4j
public class LoggingEmailSender implements EmailSender {

	@Override
	public void send(String to, String subject, String html) {
		log.info("Email disabled. subject={}", subject);
	}
}
