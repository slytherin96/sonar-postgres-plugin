package com.premiumminds.sonar.plpgsql.rules;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import jakarta.json.JsonObject;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

public class RenameStmt implements Stmt {

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange, JsonObject jsonObject) {
        final String renameType = jsonObject.getString("renameType");
        RuleKey rule;
        String message;
        switch (renameType){
            case "OBJECT_COLUMN":
                rule = PlPgSqlRulesDefinition.RULE_RENAMING_COLUMN;
                message = "Renaming a column may break existing clients.";
                break;
            case "OBJECT_TABLE":
                rule = PlPgSqlRulesDefinition.RULE_RENAMING_TABLE;
                message = "Renaming a table may break existing clients that depend on the old table name.";
                break;
            default:
                return;
        }
        NewIssue newIssue = context.newIssue()
                .forRule(rule);
        NewIssueLocation primaryLocation = newIssue.newLocation()
                .on(file)
                .at(textRange)
                .message(message);
        newIssue.at(primaryLocation);
        newIssue.save();

    }
}
