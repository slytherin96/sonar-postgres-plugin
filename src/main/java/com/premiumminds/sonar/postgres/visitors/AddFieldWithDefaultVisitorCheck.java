package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.ConstrType;
import com.premiumminds.sonar.postgres.protobuf.Constraint;
import com.premiumminds.sonar.postgres.protobuf.Node;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_ADD_FIELD_WITH_DEFAULT;
import static com.premiumminds.sonar.postgres.protobuf.ConstrType.CONSTR_DEFAULT;

public class AddFieldWithDefaultVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visitAlterTableColumnConstraint(Constraint constraint) {
        final ConstrType contype = constraint.getContype();
        if (CONSTR_DEFAULT.equals(contype)){
            final Node rawExpr = constraint.getRawExpr();
            if (rawExpr.hasFuncCall()){
                NewIssue newIssue = getContext().newIssue()
                        .forRule(RULE_ADD_FIELD_WITH_DEFAULT);
                NewIssueLocation primaryLocation = newIssue.newLocation()
                        .on(getFile())
                        .at(getTextRange())
                        .message("Adding a field with a VOLATILE default can cause table rewrites, which will take an ACCESS EXCLUSIVE lock on the table, blocking reads / writes while the statement is running.");
                newIssue.at(primaryLocation);
                newIssue.save();
            }
        }

        super.visitAlterTableColumnConstraint(constraint);
    }
}
