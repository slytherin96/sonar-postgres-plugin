package com.premiumminds.sonar.plpgsql.rules;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import jakarta.json.JsonObject;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

public class CreateStmt implements Stmt {

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange, JsonObject jsonObject) {
        if(!jsonObject.getBoolean("if_not_exists", false)){

            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_IF_NOT_EXISTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF NOT EXISTS");
            newIssue.at(primaryLocation);
            newIssue.save();

        }
    }
}
