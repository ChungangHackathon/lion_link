package com.lionlink.backend.domain.profile.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Collection;
import java.util.HashSet;

public class DistinctItemsValidator implements ConstraintValidator<DistinctItems, Collection<?>> {

	@Override
	public boolean isValid(Collection<?> items, ConstraintValidatorContext context) {
		return items == null || new HashSet<>(items).size() == items.size();
	}
}
