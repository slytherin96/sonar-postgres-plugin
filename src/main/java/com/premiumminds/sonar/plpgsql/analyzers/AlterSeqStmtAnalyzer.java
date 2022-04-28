package com.premiumminds.sonar.plpgsql.analyzers;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import com.premiumminds.sonar.postgres.protobuf.AlterSeqStmt;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

public class AlterSeqStmtAnalyzer implements Analyzer {
    private final AlterSeqStmt alterSeqStmt;

    public AlterSeqStmtAnalyzer(AlterSeqStmt alterSeqStmt) {
        this.alterSeqStmt = alterSeqStmt;
    }

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange) {
        if (!alterSeqStmt.getMissingOk()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF EXISTS to ALTER SEQUENCE " + alterSeqStmt.getSequence().getRelname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }
}
