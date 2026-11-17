package db.migration;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V3__Drop_enum_checks extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		List<String> statements = new ArrayList<>();
		try (Statement query = context.getConnection().createStatement();
				ResultSet checks = query.executeQuery("""
						select table_name, constraint_name from information_schema.table_constraints
						where constraint_schema = database() and constraint_type = 'CHECK'
						""")) {
			while (checks.next()) {
				statements.add("alter table `" + checks.getString(1) + "` drop check `" + checks.getString(2) + "`");
			}
		}
		try (Statement update = context.getConnection().createStatement()) {
			for (String statement : statements) {
				update.execute(statement);
			}
		}
	}
}
