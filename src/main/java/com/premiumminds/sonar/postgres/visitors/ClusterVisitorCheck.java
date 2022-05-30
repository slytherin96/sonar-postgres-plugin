package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.ClusterStmt;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_CLUSTER;

@Rule(key = "cluster")
public class ClusterVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(ClusterStmt clusterStmt) {
        newIssue("CLUSTER exclusively locks the table while running");
        super.visit(clusterStmt);
    }

    @Override
    protected RuleKey getRule() {
        return RULE_CLUSTER;
    }
}
