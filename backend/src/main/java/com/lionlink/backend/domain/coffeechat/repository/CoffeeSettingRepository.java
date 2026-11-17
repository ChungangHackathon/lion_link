package com.lionlink.backend.domain.coffeechat.repository;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoffeeSettingRepository extends JpaRepository<CoffeeSetting, Long> {
}
