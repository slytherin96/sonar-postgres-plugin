package com.premiumminds.sonar.plpgsql.analyzers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import com.premiumminds.sonar.postgres.protobuf.DropStmt;
import com.premiumminds.sonar.postgres.protobuf.ObjectType;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

public class DropStmtAnalyzer implements Analyzer {

    private final DropStmt dropStmt;

    public DropStmtAnalyzer(DropStmt dropStmt) {

        this.dropStmt = dropStmt;
    }

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange) {

        final List<String> names = dropStmt.getObjectsList()
                .stream()
                .map(y -> y.getList()
                        .getItemsList())
                .flatMap(List::stream)
                .map(x -> x.getString().getStr())
                .collect(Collectors.toList());

        if(!dropStmt.getMissingOk()){
            String message;
            final ObjectType removeType = dropStmt.getRemoveType();
            switch (removeType){
                case OBJECT_TABLE:
                    message = "Add IF EXISTS to DROP TABLE " + String.join(", ", names);
                    break;
                case OBJECT_SEQUENCE:
                    message = "Add IF EXISTS to DROP SEQUENCE " + String.join(", ", names);
                    break;
                case OBJECT_INDEX:
                    message = "Add IF EXISTS to DROP INDEX " + String.join(", ", names);
                    break;
                default:
                    message = "Add IF EXISTS";
                    break;
            }

            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message(message);
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        if (dropStmt.getRemoveType().equals(ObjectType.OBJECT_INDEX) && !dropStmt.getConcurrent()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_CONCURRENTLY);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add CONCURRENTLY to DROP INDEX " + String.join(", ", names));
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }
}
