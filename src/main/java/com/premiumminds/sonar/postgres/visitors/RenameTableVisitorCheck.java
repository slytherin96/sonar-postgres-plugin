package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.ObjectType;
import com.premiumminds.sonar.postgres.protobuf.RenameStmt;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_RENAMING_TABLE;
import static com.premiumminds.sonar.postgres.protobuf.ObjectType.OBJECT_TABLE;

@Rule(key = "renaming-table")
public class RenameTableVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(RenameStmt renameStmt) {
        final ObjectType renameType = renameStmt.getRenameType();
        if (renameType.equals(OBJECT_TABLE)){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(RULE_RENAMING_TABLE);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Renaming a table may break existing clients that depend on the old table name.");
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        super.visit(renameStmt);
    }
}
