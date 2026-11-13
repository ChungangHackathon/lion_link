package com.lionlink.backend.domain.profile.basic.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lionlink.backend.domain.profile.Interest;
import com.lionlink.backend.domain.profile.LionPart;
import com.lionlink.backend.domain.profile.ProfileStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;

/** PROFILE-01. 이름·기수는 받지 않고 가입 정보를 그대로 쓴다. */
public record OnboardingRequest(
		@NotEmpty(message = "멋사 파트를 선택해 주세요.")
		@Size(max = 2, message = "멋사 파트는 최대 2개까지 선택할 수 있어요.") List<@NotNull(message = "멋사 파트를 선택해 주세요.") LionPart> lionParts,
		@NotNull(message = "현재 상태를 선택해 주세요.") ProfileStatus status,
		@NotEmpty(message = "관심 분야를 선택해 주세요.")
		@Size(max = 3, message = "관심 분야는 최대 3개까지 선택할 수 있어요.") List<@NotNull(message = "관심 분야를 선택해 주세요.") Interest> interests,
		@Size(min = 1, max = 30, message = "회사명은 1~30자로 입력해 주세요.") String company,
		@Size(min = 1, max = 30, message = "직무는 1~30자로 입력해 주세요.") String jobTitle,
		@Size(max = 500, message = "링크는 500자 이하로 입력해 주세요.")
		@Pattern(regexp = "^https?://\\S+$", message = "http:// 또는 https://로 시작하는 주소를 입력해 주세요.") String link,
		@Size(max = 100, message = "한 줄 소개는 100자 이하로 입력해 주세요.") String headline,
		@Size(max = 5, message = "운영진 활동은 최대 5개까지 입력할 수 있어요.") List<@Valid @NotNull(message = "운영진 활동을 입력해 주세요.") StaffActivity> staffActivities
) {

	public record StaffActivity(
			@NotNull(message = "기수를 선택해 주세요.")
			@Min(value = 1, message = "기수는 1 이상이어야 해요.")
			@Max(value = 14, message = "기수는 14 이하여야 해요.") Integer generation,
			@NotEmpty(message = "파트를 선택해 주세요.")
			@Size(max = 2, message = "파트는 최대 2개까지 선택할 수 있어요.") List<@NotNull(message = "파트를 선택해 주세요.") LionPart> parts
	) {

		@JsonIgnore
		@AssertTrue(message = "같은 파트를 두 번 선택할 수 없어요.")
		public boolean isPartsDistinct() {
			return parts == null || new HashSet<>(parts).size() == parts.size();
		}
	}

	@JsonIgnore
	@AssertTrue(message = "같은 멋사 파트를 두 번 선택할 수 없어요.")
	public boolean isLionPartsDistinct() {
		return lionParts == null || new HashSet<>(lionParts).size() == lionParts.size();
	}

	@JsonIgnore
	@AssertTrue(message = "같은 관심 분야를 두 번 선택할 수 없어요.")
	public boolean isInterestsDistinct() {
		return interests == null || new HashSet<>(interests).size() == interests.size();
	}

	@JsonIgnore
	@AssertTrue(message = "재직 중이면 회사명과 직무를 입력해 주세요.")
	public boolean isEmployedInfoPresent() {
		return status != ProfileStatus.EMPLOYED
				|| (company != null && !company.isBlank() && jobTitle != null && !jobTitle.isBlank());
	}

	@JsonIgnore
	@AssertTrue(message = "같은 기수의 운영진 활동을 두 번 입력할 수 없어요.")
	public boolean isStaffGenerationsDistinct() {
		if (staffActivities == null) {
			return true;
		}
		List<Integer> generations = staffActivities.stream()
				.filter(activity -> activity != null && activity.generation() != null)
				.map(StaffActivity::generation)
				.toList();
		return new HashSet<>(generations).size() == generations.size();
	}
}
