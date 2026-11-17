package com.lionlink.backend.domain.notification.repository;

import com.lionlink.backend.domain.notification.entity.EmailLog;
import com.lionlink.backend.domain.notification.entity.EmailStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

	/** 보낼 시각이 된 메일을 먼저 쌓인 순서로 */
	@Query("""
			select e from EmailLog e
			where e.status = :status and e.scheduledAt <= :now
			order by e.scheduledAt asc, e.id asc
			""")
	List<EmailLog> findDue(EmailStatus status, LocalDateTime now, Pageable pageable);
}
