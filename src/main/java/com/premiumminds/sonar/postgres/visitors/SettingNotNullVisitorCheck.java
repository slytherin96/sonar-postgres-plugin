package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.AlterTableCmd;
import com.premiumminds.sonar.postgres.protobuf.AlterTableType;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_SETTING_NOT_NULLABLE_FIELD;
import static com.premiumminds.sonar.postgres.protobuf.AlterTableType.AT_SetNotNull;

public class SettingNotNullVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(AlterTableCmd alterTableCmd) {

        final AlterTableType subtype = alterTableCmd.getSubtype();
        if (AT_SetNotNull.equals(subtype)){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(RULE_SETTING_NOT_NULLABLE_FIELD);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Setting a column as NOT NULL will require a scan of the entire table. However, if a valid CHECK constraint is found which proves no NULL can exist, then the table scan is skipped.");
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        super.visit(alterTableCmd);
    }
}
