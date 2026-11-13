package com.lionlink.backend.domain.notification.repository;

import com.lionlink.backend.domain.notification.entity.Notification;
import com.lionlink.backend.domain.notification.entity.NotificationTab;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findTop20ByUserIdOrderByCreatedAtDescIdDesc(Long userId);

	List<Notification> findTop20ByUserIdAndTabOrderByCreatedAtDescIdDesc(Long userId, NotificationTab tab);

	Optional<Notification> findByIdAndUserId(Long id, Long userId);

	@Query("""
			select new com.lionlink.backend.domain.notification.repository.TabCount(n.tab, count(n))
			from Notification n
			where n.userId = :userId and n.readAt is null
			group by n.tab
			""")
	List<TabCount> countUnreadByTab(Long userId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
	int readAll(Long userId, LocalDateTime now);
}
