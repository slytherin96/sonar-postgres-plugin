package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.AlterTableCmd;
import com.premiumminds.sonar.postgres.protobuf.AlterTableType;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_SETTING_NOT_NULLABLE_FIELD;
import static com.premiumminds.sonar.postgres.protobuf.AlterTableType.AT_SetNotNull;

@Rule(key = "setting-not-nullable-field")
public class SettingNotNullVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(AlterTableCmd alterTableCmd) {

        final AlterTableType subtype = alterTableCmd.getSubtype();
        if (AT_SetNotNull.equals(subtype)){
            newIssue("Setting a column as NOT NULL will require a scan of the entire table. However, if a valid CHECK constraint is found which proves no NULL can exist, then the table scan is skipped.");
        }

        super.visit(alterTableCmd);
    }

    @Override
    protected RuleKey getRule() {
        return RULE_SETTING_NOT_NULLABLE_FIELD;
    }
}
