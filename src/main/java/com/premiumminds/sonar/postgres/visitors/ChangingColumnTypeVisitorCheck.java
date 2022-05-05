package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.AlterTableCmd;
import com.premiumminds.sonar.postgres.protobuf.AlterTableType;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_CHANGING_COLUMN_TYPE;
import static com.premiumminds.sonar.postgres.protobuf.AlterTableType.AT_AlterColumnType;

@Rule(key = "changing-column-type")
public class ChangingColumnTypeVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(AlterTableCmd alterTableCmd) {

        final AlterTableType subtype = alterTableCmd.getSubtype();
        if (AT_AlterColumnType.equals(subtype)){
            newIssue("Changing a column type requires an ACCESS EXCLUSIVE lock on the table which blocks reads and writes while the table is rewritten.");
        }

        super.visit(alterTableCmd);
    }

    @Override
    protected RuleKey getRule() {
        return RULE_CHANGING_COLUMN_TYPE;
    }
}
