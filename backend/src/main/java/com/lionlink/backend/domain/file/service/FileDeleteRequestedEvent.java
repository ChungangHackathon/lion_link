package com.lionlink.backend.domain.file.service;

import java.util.List;

public record FileDeleteRequestedEvent(List<String> urls) {
}
