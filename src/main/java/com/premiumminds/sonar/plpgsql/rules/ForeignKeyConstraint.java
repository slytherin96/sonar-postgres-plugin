package com.premiumminds.sonar.plpgsql.rules;

import jakarta.json.JsonObject;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_ADD_FOREIGN_KEY;

public class ForeignKeyConstraint implements Constraint {

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange, JsonObject constraintJson) {
        final boolean initially_valid = constraintJson.getBoolean("initially_valid", false);
        final boolean skip_validation = constraintJson.getBoolean("skip_validation", false);
        if (initially_valid && !skip_validation){
            NewIssue newIssue = context.newIssue()
                    .forRule(RULE_ADD_FOREIGN_KEY);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Adding a column with a volatile DEFAULT or changing the type of an existing column will require the entire table and its indexes to be rewritten");
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }
}
