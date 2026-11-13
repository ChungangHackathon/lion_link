package com.lionlink.backend.domain.coffeechat.repository;

import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMember;
import com.lionlink.backend.domain.coffeechat.entity.CoffeeChatMemberId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoffeeChatMemberRepository extends JpaRepository<CoffeeChatMember, CoffeeChatMemberId> {

	List<CoffeeChatMember> findByCoffeeChatIdIn(Collection<Long> coffeeChatIds);

	Optional<CoffeeChatMember> findByCoffeeChatIdAndUserId(Long coffeeChatId, Long userId);
}
