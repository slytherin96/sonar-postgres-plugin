package com.premiumminds.sonar.plpgsql.analyzers;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import com.premiumminds.sonar.plpgsql.protobuf.DropStmt;
import com.premiumminds.sonar.plpgsql.protobuf.ObjectType;
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

        if(!dropStmt.getMissingOk()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF NOT EXISTS");
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        if (dropStmt.getRemoveType().equals(ObjectType.OBJECT_INDEX) && !dropStmt.getConcurrent()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_CONCURRENTLY);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add CONCURRENTLY");
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }
}
