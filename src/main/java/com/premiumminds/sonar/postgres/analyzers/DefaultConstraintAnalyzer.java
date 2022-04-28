package com.premiumminds.sonar.postgres.analyzers;

import com.premiumminds.sonar.postgres.protobuf.Constraint;
import com.premiumminds.sonar.postgres.protobuf.Node;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_ADD_FIELD_WITH_DEFAULT;

public class DefaultConstraintAnalyzer implements ConstraintAnalyzer {

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange, Constraint constraint) {
        final Node rawExpr = constraint.getRawExpr();
        if (rawExpr.hasFuncCall()){
            NewIssue newIssue = context.newIssue()
                    .forRule(RULE_ADD_FIELD_WITH_DEFAULT);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Adding a field with a VOLATILE default can cause table rewrites, which will take an ACCESS EXCLUSIVE lock on the table, blocking reads / writes while the statement is running.");
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }
}
