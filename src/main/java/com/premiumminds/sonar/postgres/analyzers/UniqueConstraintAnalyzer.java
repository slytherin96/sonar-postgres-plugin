package com.premiumminds.sonar.postgres.analyzers;

import com.premiumminds.sonar.postgres.protobuf.Constraint;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.premiumminds.sonar.postgres.PlPgSqlRulesDefinition.RULE_DISALLOWED_UNIQUE_CONSTRAINT;

public class UniqueConstraintAnalyzer implements ConstraintAnalyzer {

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange, Constraint constraint) {
        if (constraint.getIndexname().isEmpty()){
            NewIssue newIssue = context.newIssue()
                    .forRule(RULE_DISALLOWED_UNIQUE_CONSTRAINT);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Adding a UNIQUE constraint requires an ACCESS EXCLUSIVE lock which blocks reads and writes to the table while the index is built.");
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }
}
