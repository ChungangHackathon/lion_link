package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V2__Catch_up_first_deploy extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (Statement statement = connection.createStatement()) {
			if (!hasColumn(connection, "chat_rooms", "meetup_id")) {
				statement.execute("alter table `chat_rooms` add column `meetup_id` bigint default null");
			}
			if (!hasColumn(connection, "chat_rooms", "closed_at")) {
				statement.execute("alter table `chat_rooms` add column `closed_at` datetime(6) default null");
			}
			if (!hasIndex(connection, "chat_rooms", "uk_chat_rooms_meetup")) {
				statement.execute("alter table `chat_rooms` add unique key `uk_chat_rooms_meetup` (`meetup_id`)");
			}
			statement.execute("""
					create table if not exists `post_search_keywords` (
					  `id` bigint not null auto_increment,
					  `searched_at` datetime(6) not null,
					  `user_id` bigint not null,
					  `keyword` varchar(20) collate utf8mb4_unicode_ci not null,
					  primary key (`id`),
					  unique key `uk_post_search_keywords_user_keyword` (`user_id`, `keyword`),
					  key `idx_post_search_keywords_searched_at` (`searched_at`)
					) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
					""");
		}
	}

	private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
		return exists(connection, """
				select 1 from information_schema.columns
				where table_schema = database() and table_name = ? and column_name = ?
				""", table, column);
	}

	private boolean hasIndex(Connection connection, String table, String index) throws SQLException {
		return exists(connection, """
				select 1 from information_schema.statistics
				where table_schema = database() and table_name = ? and index_name = ?
				""", table, index);
	}

	private boolean exists(Connection connection, String sql, String first, String second) throws SQLException {
		try (PreparedStatement query = connection.prepareStatement(sql)) {
			query.setString(1, first);
			query.setString(2, second);
			try (ResultSet result = query.executeQuery()) {
				return result.next();
			}
		}
	}
}
