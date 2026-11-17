package com.lionlink.backend.global.security;

import java.util.Optional;

public interface MemberAccessReader {

	Optional<MemberAccess> find(Long userId);
}
