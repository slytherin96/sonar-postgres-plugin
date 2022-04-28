package com.premiumminds.sonar.postgres.analyzers;

import com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition;
import com.premiumminds.sonar.postgres.protobuf.IndexStmt;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

public class IndexStmtAnalyzer implements Analyzer {

    private final IndexStmt indexStmt;

    public IndexStmtAnalyzer(IndexStmt indexStmt) {

        this.indexStmt = indexStmt;
    }

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange) {

        if (!indexStmt.getIfNotExists()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF NOT EXISTS to CREATE INDEX " + indexStmt.getIdxname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        if (!indexStmt.getConcurrent()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_CONCURRENTLY);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add CONCURRENTLY to CREATE INDEX " + indexStmt.getIdxname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }
}
