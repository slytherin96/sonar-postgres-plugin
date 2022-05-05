package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.ConstrType;
import com.premiumminds.sonar.postgres.protobuf.Constraint;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_ADDING_SERIAL_PRIMARY_KEY_FIELD;

@Rule(key = "adding-serial-primary-key-field")
public class AddingSerialPrimaryKeyfieldvisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visitAlterTableTableConstraint(Constraint constraint) {
        final ConstrType contype = constraint.getContype();
        if (ConstrType.CONSTR_PRIMARY.equals(contype) && constraint.getIndexname().isEmpty()){
            newIssue("If PRIMARY KEY is specified, and the index's columns are not already marked NOT NULL, then this command will attempt to do ALTER COLUMN SET NOT NULL against each such column. That requires a full table scan to verify the column(s) contain no nulls. In all other cases, this is a fast operation.");
        }

        super.visitAlterTableTableConstraint(constraint);
    }

    @Override
    protected RuleKey getRule() {
        return RULE_ADDING_SERIAL_PRIMARY_KEY_FIELD;
    }
}
