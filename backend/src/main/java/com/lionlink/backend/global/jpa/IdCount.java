package com.lionlink.backend.global.jpa;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public interface IdCount {

	Long getId();

	Long getCount();

	static Map<Long, Long> toMap(Collection<IdCount> counts) {
		return counts.stream().collect(Collectors.toMap(IdCount::getId, IdCount::getCount));
	}
}
