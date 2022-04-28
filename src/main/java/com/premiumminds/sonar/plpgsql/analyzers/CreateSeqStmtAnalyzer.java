package com.premiumminds.sonar.plpgsql.analyzers;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import com.premiumminds.sonar.plpgsql.protobuf.CreateSeqStmt;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

public class CreateSeqStmtAnalyzer implements Analyzer {

    private final CreateSeqStmt createSeqStmt;

    public CreateSeqStmtAnalyzer(CreateSeqStmt createSeqStmt) {

        this.createSeqStmt = createSeqStmt;
    }

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange) {
        if (!createSeqStmt.getIfNotExists()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF NOT EXISTS to CREATE SEQUENCE " + createSeqStmt.getSequence().getRelname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }
}
