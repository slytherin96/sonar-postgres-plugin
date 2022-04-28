package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.ConstrType;
import com.premiumminds.sonar.postgres.protobuf.Constraint;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_ADD_FOREIGN_KEY;

@Rule(key = "adding-foreign-key-constraint")
public class AddForeignKeyVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visitAlterTableTableConstraint(Constraint constraint) {
        visitConstraint(constraint);

        super.visitAlterTableTableConstraint(constraint);
    }

    @Override
    public void visitAlterTableColumnConstraint(Constraint constraint) {
        visitConstraint(constraint);

        super.visitAlterTableColumnConstraint(constraint);
    }

    @Override
    public void visitCreateTableColumnConstraint(Constraint constraint) {
        visitConstraint(constraint);

        super.visitCreateTableColumnConstraint(constraint);
    }

    @Override
    public void visitCreateTableTableConstraint(Constraint constraint) {
        visitConstraint(constraint);

        super.visitCreateTableTableConstraint(constraint);
    }

    private void visitConstraint(Constraint constraint) {
        final ConstrType contype = constraint.getContype();
        if (ConstrType.CONSTR_FOREIGN.equals(contype) && constraint.getInitiallyValid() && !constraint.getSkipValidation()){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(RULE_ADD_FOREIGN_KEY);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Adding a foreign key constraint requires a table scan and a SHARE ROW EXCLUSIVE lock on both tables, which blocks writes to each table.");
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }
}
