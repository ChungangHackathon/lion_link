SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `activities` (
  `end_ym` date DEFAULT NULL,
  `start_ym` date DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `awards` (
  `date` date DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `bookmarks` (
  `created_at` datetime(6) NOT NULL,
  `target_user_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`target_user_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `careers` (
  `end_ym` date DEFAULT NULL,
  `is_current` bit(1) NOT NULL,
  `start_ym` date DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `employment_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `company` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `certifications` (
  `date` date DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `organization` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_messages` (
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `edited_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` bigint NOT NULL,
  `sender_id` bigint DEFAULT NULL,
  `type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `client_msg_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chat_messages_client_msg` (`sender_id`,`client_msg_id`),
  KEY `FKhalwepod3944695ji0suwoqb9` (`room_id`),
  CONSTRAINT `FKhalwepod3944695ji0suwoqb9` FOREIGN KEY (`room_id`) REFERENCES `chat_rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_room_members` (
  `muted` bit(1) NOT NULL,
  `cleared_message_id` bigint DEFAULT NULL,
  `joined_at` datetime(6) NOT NULL,
  `last_read_message_id` bigint DEFAULT NULL,
  `left_at` datetime(6) DEFAULT NULL,
  `pinned_at` datetime(6) DEFAULT NULL,
  `room_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`room_id`,`user_id`),
  CONSTRAINT `FKdvub8k7sypahkamqjaiokb44t` FOREIGN KEY (`room_id`) REFERENCES `chat_rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_rooms` (
  `accepted_at` datetime(6) DEFAULT NULL,
  `closed_at` datetime(6) DEFAULT NULL,
  `coffee_chat_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `declined_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `last_message_at` datetime(6) DEFAULT NULL,
  `meetup_id` bigint DEFAULT NULL,
  `requested_at` datetime(6) DEFAULT NULL,
  `requested_by` bigint DEFAULT NULL,
  `user_a_id` bigint DEFAULT NULL,
  `user_b_id` bigint DEFAULT NULL,
  `type` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `decline_reason` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `decline_message` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chat_rooms_direct` (`user_a_id`,`user_b_id`),
  UNIQUE KEY `uk_chat_rooms_coffee_chat` (`coffee_chat_id`),
  UNIQUE KEY `uk_chat_rooms_meetup` (`meetup_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `coffee_chat_members` (
  `coffee_chat_id` bigint NOT NULL,
  `joined_at` datetime(6) NOT NULL,
  `left_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`coffee_chat_id`,`user_id`),
  CONSTRAINT `FKimv80h5h1jycy5q9ka6oym80d` FOREIGN KEY (`coffee_chat_id`) REFERENCES `coffee_chats` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `coffee_chats` (
  `open_flag` int DEFAULT NULL,
  `accepted_at` datetime(6) DEFAULT NULL,
  `canceled_at` datetime(6) DEFAULT NULL,
  `canceled_by` bigint DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receiver_id` bigint NOT NULL,
  `rejected_at` datetime(6) DEFAULT NULL,
  `requester_id` bigint NOT NULL,
  `scheduled_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `meeting_mode` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `preferred_mode` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reject_reason` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `topic` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `meeting_place` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `topic_custom` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `schedule_note` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cancel_reason` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `response_message` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `meeting_link` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `proposed_slots` json DEFAULT NULL,
  `questions` json NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coffee_chats_open` (`requester_id`,`receiver_id`,`open_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `coffee_periods` (
  `end_date` date NOT NULL,
  `start_date` date NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `coffee_settings` (
  `accept_group` bit(1) NOT NULL,
  `user_id` bigint NOT NULL,
  `default_mode` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `preferred_area` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `available_slots` json DEFAULT NULL,
  `modes` json NOT NULL,
  `topics` json DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `coffeechat_recruits` (
  `capacity` int NOT NULL,
  `deadline` date NOT NULL,
  `coffee_chat_id` bigint DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `senior_id` bigint NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`post_id`),
  CONSTRAINT `FK6fvpgvdt4k75lq1abyljenn33` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comments` (
  `anon_no` int DEFAULT NULL,
  `is_anonymous` bit(1) NOT NULL,
  `author_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT NULL,
  `post_id` bigint NOT NULL,
  `content` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKlri30okf66phtcgbe5pok7cc0` (`parent_id`),
  KEY `FKh4c7lvsc298whoyd4w9ta25cr` (`post_id`),
  CONSTRAINT `FKh4c7lvsc298whoyd4w9ta25cr` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`),
  CONSTRAINT `FKlri30okf66phtcgbe5pok7cc0` FOREIGN KEY (`parent_id`) REFERENCES `comments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `educations` (
  `end_ym` date DEFAULT NULL,
  `is_current` bit(1) NOT NULL,
  `start_ym` date DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `degree` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `major` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `school` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `email_logs` (
  `attempt_count` int NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scheduled_at` datetime(6) NOT NULL,
  `sent_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `to_email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_ids` json DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `keyword_blocklist` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `word` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `languages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `level` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `language` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_languages_user_language` (`user_id`,`language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `lion_activities` (
  `generation` int NOT NULL,
  `is_default` bit(1) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `part_1` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `part_2` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lion_activities_user_generation` (`user_id`,`generation`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `lion_projects` (
  `generation` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `event_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `my_role` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `link` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `meetup_invites` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `invitee_id` bigint NOT NULL,
  `inviter_id` bigint NOT NULL,
  `meetup_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKdohofqplxd3gdd70dpbcv6c7c` (`meetup_id`),
  CONSTRAINT `FKdohofqplxd3gdd70dpbcv6c7c` FOREIGN KEY (`meetup_id`) REFERENCES `meetups` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `meetup_members` (
  `joined_at` datetime(6) NOT NULL,
  `meetup_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`meetup_id`,`user_id`),
  CONSTRAINT `FKr5f7m6dr45bum8e1q9wgy53ry` FOREIGN KEY (`meetup_id`) REFERENCES `meetups` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `meetups` (
  `capacity` int NOT NULL,
  `canceled_at` datetime(6) DEFAULT NULL,
  `meet_at` datetime(6) NOT NULL,
  `post_id` bigint NOT NULL,
  `place` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`post_id`),
  CONSTRAINT `FKllrhkensd7v3vnms50udqagkw` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notification_settings` (
  `email_coffee_chat` bit(1) NOT NULL,
  `email_message_request` bit(1) NOT NULL,
  `email_signup_approved` bit(1) NOT NULL DEFAULT b'1',
  `user_id` bigint NOT NULL,
  `unsubscribe_token` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_notification_settings_unsubscribe_token` (`unsubscribe_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notifications` (
  `actor_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `read_at` datetime(6) DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `tab` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `post_files` (
  `size` int NOT NULL,
  `sort_order` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8xc9ew2bxk7axrmc9epq40v2h` (`post_id`),
  CONSTRAINT `FK8xc9ew2bxk7axrmc9epq40v2h` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `post_likes` (
  `created_at` datetime(6) NOT NULL,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`post_id`,`user_id`),
  CONSTRAINT `FKa5wxsgl4doibhbed9gm7ikie2` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `post_search_keywords` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `searched_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `keyword` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_search_keywords_user_keyword` (`user_id`,`keyword`),
  KEY `idx_post_search_keywords_searched_at` (`searched_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `post_views` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `viewed_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKm1fm9hc7487k4j6qd2g1iq0k2` (`post_id`),
  CONSTRAINT `FKm1fm9hc7487k4j6qd2g1iq0k2` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `posts` (
  `deadline` date DEFAULT NULL,
  `is_anonymous` bit(1) NOT NULL,
  `view_count` int NOT NULL,
  `author_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `category` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `links` json DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `profiles` (
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `company_visibility` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email_visibility` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `links_visibility` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lion_part_1` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lion_part_2` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone_visibility` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `region` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `major` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `minor` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `headline` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `photo_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bio` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `interests` json NOT NULL,
  `links` json DEFAULT NULL,
  `skills` json DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `projects` (
  `end_ym` date DEFAULT NULL,
  `is_current` bit(1) NOT NULL,
  `start_ym` date DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `link` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tags` json DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `recruit_members` (
  `joined_at` datetime(6) NOT NULL,
  `recruit_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`recruit_id`,`user_id`),
  CONSTRAINT `FKs50jaf42abds176orehbemi1f` FOREIGN KEY (`recruit_id`) REFERENCES `coffeechat_recruits` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `refresh_tokens` (
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `revoked_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `token_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_tokens_token_hash` (`token_hash`),
  KEY `idx_refresh_tokens_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `users` (
  `generation` int NOT NULL,
  `profile_completed` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `guide_dismissed_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reapplied_at` datetime(6) DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `reviewed_by` bigint DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `name` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `google_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reject_reason` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `google_sub` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_google_sub` (`google_sub`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
