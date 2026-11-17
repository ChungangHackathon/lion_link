package com.lionlink.backend.domain.user.repository;

/** 회원 한 명의 id·이름·기수만 담은 조회용 값. 엔티티 전체를 읽지 않고 대조에만 쓴다. */
public record UserIdentity(Long id, String name, Integer generation) {
}
