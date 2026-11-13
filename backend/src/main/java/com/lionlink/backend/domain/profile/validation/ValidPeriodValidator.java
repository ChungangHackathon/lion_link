package com.lionlink.backend.domain.profile.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.YearMonth;
import java.time.ZoneId;

public class ValidPeriodValidator implements ConstraintValidator<ValidPeriod, PeriodRequest> {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private boolean requireEndOrCurrent;

	@Override
	public void initialize(ValidPeriod annotation) {
		this.requireEndOrCurrent = annotation.requireEndOrCurrent();
	}

	@Override
	public boolean isValid(PeriodRequest request, ConstraintValidatorContext context) {
		if (request == null) {
			return true;
		}
		YearMonth start = request.startYm();
		YearMonth end = request.endYm();
		boolean current = Boolean.TRUE.equals(request.isCurrent());
		context.disableDefaultConstraintViolation();
		boolean valid = true;

		if (start != null && start.isAfter(YearMonth.now(KST))) {
			valid = violation(context, "startYm", "시작 년월은 이번 달까지만 입력할 수 있어요.");
		}
		if (current && end != null) {
			valid = violation(context, "endYm", "진행 중이면 종료 년월을 입력할 수 없어요.");
		} else if (end != null && start == null) {
			valid = violation(context, "startYm", "종료 년월만 입력할 수 없어요. 시작 년월도 입력해 주세요.");
		} else if (end != null && end.isBefore(start)) {
			valid = violation(context, "endYm", "종료 년월은 시작 년월보다 빠를 수 없어요.");
		}
		if (requireEndOrCurrent && !current && end == null) {
			valid = violation(context, "endYm", "종료 년월을 입력하거나 진행 중으로 표시해 주세요.");
		}
		return valid;
	}

	private static boolean violation(ConstraintValidatorContext context, String field, String message) {
		context.buildConstraintViolationWithTemplate(message)
				.addPropertyNode(field)
				.addConstraintViolation();
		return false;
	}
}
