package com.lionlink.backend.domain.coffeechat.repository;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMember;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMemberId;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMemberRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CoffeeChatMemberRepository extends JpaRepository<CoffeeChatMember, CoffeeChatMemberId> {

	List<CoffeeChatMember> findByCoffeeChatIdIn(Collection<Long> coffeeChatIds);

	Optional<CoffeeChatMember> findByCoffeeChatIdAndUserId(Long coffeeChatId, Long userId);

	@Query("select m from CoffeeChatMember m where m.userId = :userId and m.role = :role and m.leftAt is null and m.coffeeChat.openFlag = 1")
	List<CoffeeChatMember> findOpenMemberships(Long userId, CoffeeChatMemberRole role);
}
