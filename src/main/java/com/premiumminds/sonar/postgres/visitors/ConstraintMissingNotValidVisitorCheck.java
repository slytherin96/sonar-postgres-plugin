package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.ConstrType;
import com.premiumminds.sonar.postgres.protobuf.Constraint;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_CONSTRAINT_MISSING_NOT_VALID;

public class ConstraintMissingNotValidVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visitAlterTableTableConstraint(Constraint constraint) {
        final ConstrType contype = constraint.getContype();
        if (ConstrType.CONSTR_CHECK.equals(contype) && constraint.getInitiallyValid() && !constraint.getSkipValidation()){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(RULE_CONSTRAINT_MISSING_NOT_VALID);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("By default new constraints require a table scan and block writes to the table while that scan occurs.");
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        super.visitAlterTableTableConstraint(constraint);
    }

}
