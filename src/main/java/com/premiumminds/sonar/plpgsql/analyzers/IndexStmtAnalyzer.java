package com.premiumminds.sonar.plpgsql.analyzers;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import com.premiumminds.sonar.plpgsql.protobuf.IndexStmt;
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
                    .forRule(PlPgSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF NOT EXISTS to CREATE INDEX " + indexStmt.getIdxname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        if (!indexStmt.getConcurrent()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_CONCURRENTLY);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add CONCURRENTLY to CREATE INDEX " + indexStmt.getIdxname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }
}
