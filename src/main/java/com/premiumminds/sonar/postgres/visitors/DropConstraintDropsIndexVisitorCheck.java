package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition;
import com.premiumminds.sonar.postgres.protobuf.AlterTableCmd;
import com.premiumminds.sonar.postgres.protobuf.AlterTableType;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_DROP_CONSTRAINT_DROPS_INDEX;
import static com.premiumminds.sonar.postgres.protobuf.AlterTableType.AT_DropConstraint;

@Rule(key = "drop-constraint-drops-index")
public class DropConstraintDropsIndexVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(AlterTableCmd alterTableCmd) {

        final AlterTableType subtype = alterTableCmd.getSubtype();
        if (AT_DropConstraint.equals(subtype)){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(RULE_DROP_CONSTRAINT_DROPS_INDEX);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Dropping a primary or unique constraint also drops any index underlying the constraint");
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        super.visit(alterTableCmd);
    }

}
