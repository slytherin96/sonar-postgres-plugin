package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.AlterTableCmd;
import com.premiumminds.sonar.postgres.protobuf.AlterTableType;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_DROP_CONSTRAINT_DROPS_INDEX;
import static com.premiumminds.sonar.postgres.protobuf.AlterTableType.AT_DropConstraint;

@Rule(key = "drop-constraint-drops-index")
public class DropConstraintDropsIndexVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(AlterTableCmd alterTableCmd) {

        final AlterTableType subtype = alterTableCmd.getSubtype();
        if (AT_DropConstraint.equals(subtype)){
            newIssue("Dropping a primary or unique constraint also drops any index underlying the constraint");
        }

        super.visit(alterTableCmd);
    }

    @Override
    protected RuleKey getRule() {
        return RULE_DROP_CONSTRAINT_DROPS_INDEX;
    }

}
