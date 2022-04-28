package com.premiumminds.sonar.postgres.analyzers;

import com.premiumminds.sonar.postgres.protobuf.Constraint;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_ADD_FOREIGN_KEY;

public class ForeignKeyConstraintAnalyzer implements ConstraintAnalyzer {

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange, Constraint constraint) {

        if (constraint.getInitiallyValid() && !constraint.getSkipValidation()){
            NewIssue newIssue = context.newIssue()
                    .forRule(RULE_ADD_FOREIGN_KEY);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Adding a foreign key constraint requires a table scan and a SHARE ROW EXCLUSIVE lock on both tables, which blocks writes to each table.");
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }
}
