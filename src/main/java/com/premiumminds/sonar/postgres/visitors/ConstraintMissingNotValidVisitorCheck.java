package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.ConstrType;
import com.premiumminds.sonar.postgres.protobuf.Constraint;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_CONSTRAINT_MISSING_NOT_VALID;

@Rule(key = "constraint-missing-not-valid")
public class ConstraintMissingNotValidVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visitAlterTableTableConstraint(Constraint constraint) {
        final ConstrType contype = constraint.getContype();
        if (ConstrType.CONSTR_CHECK.equals(contype) && constraint.getInitiallyValid() && !constraint.getSkipValidation()){
            newIssue("By default new constraints require a table scan and block writes to the table while that scan occurs.");
        }

        super.visitAlterTableTableConstraint(constraint);
    }

    @Override
    protected RuleKey getRule() {
        return RULE_CONSTRAINT_MISSING_NOT_VALID;
    }

}
