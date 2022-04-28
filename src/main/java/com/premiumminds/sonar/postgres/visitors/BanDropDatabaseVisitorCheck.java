package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition;
import com.premiumminds.sonar.postgres.protobuf.DropdbStmt;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

public class BanDropDatabaseVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(DropdbStmt dropdbStmt) {
        NewIssue newIssue = getContext().newIssue()
                .forRule(PostgresSqlRulesDefinition.RULE_BAN_DROP_DATABASE);
        NewIssueLocation primaryLocation = newIssue.newLocation()
                .on(getFile())
                .at(getTextRange())
                .message("Dropping a database may break existing clients.");
        newIssue.at(primaryLocation);
        newIssue.save();

        super.visit(dropdbStmt);
    }
}
