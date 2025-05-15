package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.DropBehavior;
import com.premiumminds.sonar.postgres.protobuf.TruncateStmt;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_BAN_TRUNCATE_CASCADE;

@Rule(key = "ban-truncate-cascade")
public class BanTruncateCascade extends AbstractVisitorCheck {

    @Override
    public void visit(TruncateStmt truncateStmt) {

        if (truncateStmt.getBehavior() == DropBehavior.DROP_CASCADE){
            newIssue("Truncate cascade will recursively truncate all related tables");
        }

        super.visit(truncateStmt);
    }

    @Override
    protected RuleKey getRule() {
        return RULE_BAN_TRUNCATE_CASCADE;
    }
}
