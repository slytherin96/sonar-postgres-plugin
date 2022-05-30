package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.DoStmt;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_DISALLOWED_DO;

@Rule(key = "disallowed-do")
public class DisallowedDoVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(DoStmt doStmt) {
        newIssue("DO commands can not be review by this plugin.");
        super.visit(doStmt);
    }

    @Override
    protected RuleKey getRule() {
        return RULE_DISALLOWED_DO;
    }
}
