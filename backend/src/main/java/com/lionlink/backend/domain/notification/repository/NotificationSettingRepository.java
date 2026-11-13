package com.lionlink.backend.domain.notification.repository;

import com.lionlink.backend.domain.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
}
