package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.DeleteStmt;
import com.premiumminds.sonar.postgres.protobuf.InsertStmt;
import com.premiumminds.sonar.postgres.protobuf.TruncateStmt;
import com.premiumminds.sonar.postgres.protobuf.UpdateStmt;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_ONLY_SCHEMA_MIGRATIONS;

@Rule(key = "only-schema-migrations")
public class OnlySchemaMigrationsVisitorCheck extends AbstractVisitorCheck {

	@Override
	public void visit(final InsertStmt insertStmt) {
		newIssue("INSERT statements are now allowed");
		super.visit(insertStmt);
	}

	@Override
	public void visit(final UpdateStmt updateStmt) {
		newIssue("UPDATE statements are now allowed");
		super.visit(updateStmt);
	}

	@Override
	public void visit(final DeleteStmt deleteStmt) {
		newIssue("DELETE statements are now allowed");
		super.visit(deleteStmt);
	}

	@Override
	public void visit(final TruncateStmt truncateStmt) {
		newIssue("TRUNCATE statements are now allowed");
		super.visit(truncateStmt);
	}

	@Override
	protected RuleKey getRule() {
		return RULE_ONLY_SCHEMA_MIGRATIONS;
	}
}
