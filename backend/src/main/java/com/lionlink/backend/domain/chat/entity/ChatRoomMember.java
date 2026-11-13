package com.lionlink.backend.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "chat_room_members")
@IdClass(ChatRoomMemberId.class)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id")
	private ChatRoom room;

	@Id
	private Long userId;

	private Long lastReadMessageId;

	private Long clearedMessageId;

	private boolean muted;

	private LocalDateTime pinnedAt;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime joinedAt;

	private LocalDateTime leftAt;

	@Builder
	private ChatRoomMember(ChatRoom room, Long userId) {
		this.room = room;
		this.userId = userId;
	}

	public boolean isActive() {
		return leftAt == null;
	}

	public void rejoin() {
		this.leftAt = null;
	}

	public void leave(LocalDateTime now) {
		this.leftAt = now;
	}

	public void hide(Long lastMessageId, LocalDateTime now) {
		this.leftAt = now;
		this.clearedMessageId = lastMessageId;
		this.lastReadMessageId = lastMessageId;
	}

	public void read(Long messageId) {
		if (messageId != null && (lastReadMessageId == null || messageId > lastReadMessageId)) {
			this.lastReadMessageId = messageId;
		}
	}

	public void changePreferences(Boolean pinned, Boolean muted, LocalDateTime now) {
		if (pinned != null) {
			this.pinnedAt = pinned ? (pinnedAt == null ? now : pinnedAt) : null;
		}
		if (muted != null) {
			this.muted = muted;
		}
	}

	public boolean isPinned() {
		return pinnedAt != null;
	}

	public long visibleFrom() {
		return clearedMessageId == null ? 0 : clearedMessageId;
	}
}
