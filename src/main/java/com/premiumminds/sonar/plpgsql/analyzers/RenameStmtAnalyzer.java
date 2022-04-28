package com.premiumminds.sonar.plpgsql.analyzers;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import com.premiumminds.sonar.postgres.protobuf.ObjectType;
import com.premiumminds.sonar.postgres.protobuf.RenameStmt;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

public class RenameStmtAnalyzer implements Analyzer {
    private final RenameStmt renameStmt;

    public RenameStmtAnalyzer(RenameStmt renameStmt) {
        this.renameStmt = renameStmt;
    }

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange) {

        boolean createIssue;
        RuleKey rule;
        String message;

        final ObjectType renameType = renameStmt.getRenameType();
        switch (renameType){
            case OBJECT_COLUMN:
                rule = PlPgSqlRulesDefinition.RULE_RENAMING_COLUMN;
                message = "Renaming a column may break existing clients.";
                createIssue = true;
                break;
            case OBJECT_TABLE:
                rule = PlPgSqlRulesDefinition.RULE_RENAMING_TABLE;
                message = "Renaming a table may break existing clients that depend on the old table name.";
                createIssue = true;
                break;
            case OBJECT_INDEX:
                rule = PlPgSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS;
                message = "Add IF EXISTS to ALTER INDEX " + renameStmt.getRelation().getRelname();
                createIssue = !renameStmt.getMissingOk();
                break;
            default:
                return;
        }
        if (createIssue){
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
}
