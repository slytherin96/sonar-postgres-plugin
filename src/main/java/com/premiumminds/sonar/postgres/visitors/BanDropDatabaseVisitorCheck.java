package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.DropdbStmt;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_BAN_DROP_DATABASE;

@Rule(key = "ban-drop-database")
public class BanDropDatabaseVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(DropdbStmt dropdbStmt) {
        newIssue("Dropping a database may break existing clients.");

        super.visit(dropdbStmt);
    }

    @Override
    protected RuleKey getRule() {
        return RULE_BAN_DROP_DATABASE;
    }
}
