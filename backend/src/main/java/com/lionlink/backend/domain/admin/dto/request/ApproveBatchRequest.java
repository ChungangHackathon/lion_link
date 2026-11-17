package com.lionlink.backend.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "선택 승인 요청")
public record ApproveBatchRequest(
		@Schema(description = "승인할 회원 id (1~50개, 중복 불가)")
		@NotNull(message = "승인할 회원을 선택해 주세요.")
		@NotEmpty(message = "승인할 회원을 선택해 주세요.")
		@Size(max = 50, message = "한 번에 50명까지 승인할 수 있어요.") List<Long> userIds
) {
}
