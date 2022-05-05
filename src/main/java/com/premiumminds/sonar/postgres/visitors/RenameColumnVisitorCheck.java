package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.ObjectType;
import com.premiumminds.sonar.postgres.protobuf.RenameStmt;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_RENAMING_COLUMN;
import static com.premiumminds.sonar.postgres.protobuf.ObjectType.OBJECT_COLUMN;

@Rule(key = "renaming-column")
public class RenameColumnVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(RenameStmt renameStmt) {
        final ObjectType renameType = renameStmt.getRenameType();
        if (renameType.equals(OBJECT_COLUMN)){
            newIssue("Renaming a column may break existing clients.");
        }

        super.visit(renameStmt);
    }

    @Override
    protected RuleKey getRule() {
        return RULE_RENAMING_COLUMN;
    }
}
