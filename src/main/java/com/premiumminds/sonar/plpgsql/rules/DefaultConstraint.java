package com.premiumminds.sonar.plpgsql.rules;

import jakarta.json.JsonObject;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_ADD_FIELD_WITH_DEFAULT;

public class DefaultConstraint implements Constraint {

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange, JsonObject constraintJson) {
        final JsonObject rawExpr = constraintJson.getJsonObject("raw_expr");
        if (rawExpr.containsKey("FuncCall")){
            NewIssue newIssue = context.newIssue()
                    .forRule(RULE_ADD_FIELD_WITH_DEFAULT);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Adding a column with a volatile DEFAULT or changing the type of an existing column will require the entire table and its indexes to be rewritten");
            newIssue.at(primaryLocation);
            newIssue.save();
        }

    }
}
