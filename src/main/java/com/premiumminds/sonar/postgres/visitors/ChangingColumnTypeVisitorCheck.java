package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition;
import com.premiumminds.sonar.postgres.protobuf.AlterTableCmd;
import com.premiumminds.sonar.postgres.protobuf.AlterTableType;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.premiumminds.sonar.postgres.protobuf.AlterTableType.AT_AlterColumnType;

public class ChangingColumnTypeVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(AlterTableCmd alterTableCmd) {

        final AlterTableType subtype = alterTableCmd.getSubtype();
        if (AT_AlterColumnType.equals(subtype)){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_CHANGING_COLUMN_TYPE);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Changing a column type requires an ACCESS EXCLUSIVE lock on the table which blocks reads and writes while the table is rewritten.");
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        super.visit(alterTableCmd);
    }
}
