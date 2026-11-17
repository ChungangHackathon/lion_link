package com.lionlink.backend.domain.file.service;

/** 서버가 직접 파일을 올리고 지우는 저장소. 업로드 주소 발급(FILE-01)과 같은 버킷을 쓴다. 테스트에서는 목으로 바꾼다. */
public interface FileStorage {

	/** key 위치에 파일을 올린다. 실패하면 예외를 던진다. */
	void put(String key, byte[] content, String contentType);

	/** key 위치의 파일을 지운다. 없어도 오류가 아니다. */
	void delete(String key);
}
