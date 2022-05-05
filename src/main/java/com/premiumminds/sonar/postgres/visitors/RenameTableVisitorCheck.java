package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.ObjectType;
import com.premiumminds.sonar.postgres.protobuf.RenameStmt;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_RENAMING_TABLE;
import static com.premiumminds.sonar.postgres.protobuf.ObjectType.OBJECT_TABLE;

@Rule(key = "renaming-table")
public class RenameTableVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(RenameStmt renameStmt) {
        final ObjectType renameType = renameStmt.getRenameType();
        if (renameType.equals(OBJECT_TABLE)){
            newIssue("Renaming a table may break existing clients that depend on the old table name.");
        }

        super.visit(renameStmt);
    }

    @Override
    protected RuleKey getRule() {
        return RULE_RENAMING_TABLE;
    }
}
