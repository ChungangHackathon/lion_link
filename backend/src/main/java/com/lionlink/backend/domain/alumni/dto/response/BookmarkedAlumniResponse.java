package com.lionlink.backend.domain.alumni.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.LocalDateTime;

public record BookmarkedAlumniResponse(
		@JsonUnwrapped AlumniResponse alumni,
		LocalDateTime bookmarkedAt
) {
}
