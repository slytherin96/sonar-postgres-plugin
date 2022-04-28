package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.ObjectType;
import com.premiumminds.sonar.postgres.protobuf.RenameStmt;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_RENAMING_COLUMN;
import static com.premiumminds.sonar.postgres.protobuf.ObjectType.OBJECT_COLUMN;

public class RenameColumnVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(RenameStmt renameStmt) {
        final ObjectType renameType = renameStmt.getRenameType();
        if (renameType.equals(OBJECT_COLUMN)){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(RULE_RENAMING_COLUMN);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Renaming a column may break existing clients.");
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        super.visit(renameStmt);
    }
}
