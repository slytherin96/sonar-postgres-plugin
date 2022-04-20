package com.premiumminds.sonar.plpgsql.rules;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class AlterTableStmt implements Stmt {
    private static final Logger LOGGER = Loggers.get(AlterTableStmt.class);

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange, JsonObject jsonObject) {
        final JsonArray cmds = jsonObject.getJsonArray("cmds");
        cmds.forEach(x -> {
            final JsonObject cmd = x.asJsonObject();
            final JsonObject alterTableCmd = cmd.getJsonObject("AlterTableCmd");
            final String subtype = alterTableCmd.getString("subtype");

            if(!alterTableCmd.getBoolean("missing_ok", false)){
                final RuleKey rule;
                final String message;
                switch (subtype){
                    case "AT_DropColumn":
                        rule = PlPgSqlRulesDefinition.RULE_IF_EXISTS;
                        message = "Add IF EXISTS";
                        break;
                    case "AT_AddColumn":
                        rule = PlPgSqlRulesDefinition.RULE_IF_NOT_EXISTS;
                        message = "Add IF NOT EXISTS";
                        break;
                    default:
                        LOGGER.warn(subtype + " not defined");
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
        });
    }
}
