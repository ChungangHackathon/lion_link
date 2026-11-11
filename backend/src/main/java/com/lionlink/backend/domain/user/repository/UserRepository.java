package com.lionlink.backend.domain.user.repository;

import com.lionlink.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
