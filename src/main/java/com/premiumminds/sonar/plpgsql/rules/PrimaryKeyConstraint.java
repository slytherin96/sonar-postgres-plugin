package com.premiumminds.sonar.plpgsql.rules;

import jakarta.json.JsonObject;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_ADDING_SERIAL_PRIMARY_KEY_FIELD;

public class PrimaryKeyConstraint implements Constraint {

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange, JsonObject constraintJson) {

        NewIssue newIssue = context.newIssue()
                .forRule(RULE_ADDING_SERIAL_PRIMARY_KEY_FIELD);
        NewIssueLocation primaryLocation = newIssue.newLocation()
                .on(file)
                .at(textRange)
                .message("If PRIMARY KEY is specified, and the index's columns are not already marked NOT NULL, then this command will attempt to do ALTER COLUMN SET NOT NULL against each such column. That requires a full table scan to verify the column(s) contain no nulls. In all other cases, this is a fast operation.");
        newIssue.at(primaryLocation);
        newIssue.save();

    }
}
