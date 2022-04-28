package com.premiumminds.sonar.postgres.analyzers;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import com.premiumminds.sonar.postgres.protobuf.ColumnDef;
import com.premiumminds.sonar.postgres.protobuf.TypeName;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

public class TypeNamesAnalyzer {

    void validate(SensorContext context, InputFile file, TextRange textRange, ColumnDef columnDef){

        final TypeName typeName = columnDef.getTypeName();
        typeName.getNamesList().forEach(name -> {
            final String str = name.getString().getStr();
            if ("bpchar".equals(str)){
                NewIssue newIssue = context.newIssue()
                        .forRule(PlPgSqlRulesDefinition.RULE_BAN_CHAR_FIELD);
                NewIssueLocation primaryLocation = newIssue.newLocation()
                        .on(file)
                        .at(textRange)
                        .message("Using character is likely a mistake and should almost always be replaced by text or varchar.");
                newIssue.at(primaryLocation);
                newIssue.save();
            }
            if ("varchar".equals(str) && typeName.getTypmodsList().size() != 0){
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
