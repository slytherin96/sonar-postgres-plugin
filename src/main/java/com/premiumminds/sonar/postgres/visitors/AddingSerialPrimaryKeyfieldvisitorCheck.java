package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.ConstrType;
import com.premiumminds.sonar.postgres.protobuf.Constraint;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_ADDING_SERIAL_PRIMARY_KEY_FIELD;

public class AddingSerialPrimaryKeyfieldvisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visitAlterTableTableConstraint(Constraint constraint) {
        final ConstrType contype = constraint.getContype();
        if (ConstrType.CONSTR_PRIMARY.equals(contype) && constraint.getIndexname().isEmpty()){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(RULE_ADDING_SERIAL_PRIMARY_KEY_FIELD);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("If PRIMARY KEY is specified, and the index's columns are not already marked NOT NULL, then this command will attempt to do ALTER COLUMN SET NOT NULL against each such column. That requires a full table scan to verify the column(s) contain no nulls. In all other cases, this is a fast operation.");
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        super.visitAlterTableTableConstraint(constraint);
    }
}
