package com.lionlink.backend.domain.profile.validation;

import com.lionlink.backend.domain.profile.basic.entity.Visibility;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoConnectedValidator implements ConstraintValidator<NoConnected, Visibility> {

	@Override
	public boolean isValid(Visibility value, ConstraintValidatorContext context) {
		return value != Visibility.CONNECTED;
	}
}
