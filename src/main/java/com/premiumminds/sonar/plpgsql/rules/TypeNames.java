package com.premiumminds.sonar.plpgsql.rules;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

public class TypeNames {

    void validate(SensorContext context, InputFile file, TextRange textRange, JsonObject columnDef){
        final JsonObject typeName = columnDef.getJsonObject("typeName");
        final JsonArray names = typeName.getJsonArray("names");
        names.forEach(x -> {
            final JsonString str = x.asJsonObject().getJsonObject("String").getJsonString("str");
            if (str != null && "bpchar".equals(str.getString())){
                NewIssue newIssue = context.newIssue()
                        .forRule(PlPgSqlRulesDefinition.RULE_BAN_CHAR_FIELD);
                NewIssueLocation primaryLocation = newIssue.newLocation()
                        .on(file)
                        .at(textRange)
                        .message("Using character is likely a mistake and should almost always be replaced by text or varchar.");
                newIssue.at(primaryLocation);
                newIssue.save();
            }
            if (str != null && "varchar".equals(str.getString()) && typeName.containsKey("typmods")){
                NewIssue newIssue = context.newIssue()
                        .forRule(PlPgSqlRulesDefinition.RULE_PREFER_TEXT_FIELD);
                NewIssueLocation primaryLocation = newIssue.newLocation()
                        .on(file)
                        .at(textRange)
                        .message("Changing the size of a varchar field requires an ACCESS EXCLUSIVE lock, that will prevent all reads and writes to the table.");
                newIssue.at(primaryLocation);
                newIssue.save();
            }
        });
    }
}
