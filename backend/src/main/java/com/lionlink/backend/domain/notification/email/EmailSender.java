package com.lionlink.backend.domain.notification.email;

/** 메일 한 통을 보낸다. 실패하면 예외를 던지고, 다시 보내기는 호출하는 쪽이 맡는다. */
public interface EmailSender {

	void send(String to, String subject, String html);
}
