package com.lionlink.backend.domain.profile.basic.repository;

import com.lionlink.backend.domain.profile.basic.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
}
