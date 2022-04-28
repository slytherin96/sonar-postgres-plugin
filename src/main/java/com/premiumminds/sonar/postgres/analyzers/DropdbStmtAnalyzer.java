package com.premiumminds.sonar.postgres.analyzers;

import com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition;
import com.premiumminds.sonar.postgres.protobuf.DropdbStmt;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

public class DropdbStmtAnalyzer implements Analyzer {

    private final DropdbStmt dropdbStmt;

    public DropdbStmtAnalyzer(DropdbStmt dropdbStmt) {
        this.dropdbStmt = dropdbStmt;
    }

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange) {
        NewIssue newIssue = context.newIssue()
                .forRule(PostgresSqlRulesDefinition.RULE_BAN_DROP_DATABASE);
        NewIssueLocation primaryLocation = newIssue.newLocation()
                .on(file)
                .at(textRange)
                .message("Dropping a database may break existing clients.");
        newIssue.at(primaryLocation);
        newIssue.save();
    }
}
