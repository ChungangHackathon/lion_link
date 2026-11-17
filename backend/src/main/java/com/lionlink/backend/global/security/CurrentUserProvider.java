package com.lionlink.backend.global.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface CurrentUserProvider {

	Optional<Long> findUserId(HttpServletRequest request);
}
