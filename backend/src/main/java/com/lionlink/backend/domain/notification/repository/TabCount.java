package com.lionlink.backend.domain.notification.repository;

import com.lionlink.backend.domain.notification.entity.NotificationTab;

public record TabCount(NotificationTab tab, long count) {
}
