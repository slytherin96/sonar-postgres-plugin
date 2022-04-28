package com.premiumminds.sonar.plpgsql.analyzers;

import com.premiumminds.sonar.postgres.protobuf.Constraint;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_CONSTRAINT_MISSING_NOT_VALID;

public class CheckContraintAnalyzer implements ConstraintAnalyzer {

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange, Constraint constraint) {
        if (constraint.getInitiallyValid() && !constraint.getSkipValidation()){
            NewIssue newIssue = context.newIssue()
                    .forRule(RULE_CONSTRAINT_MISSING_NOT_VALID);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("By default new constraints require a table scan and block writes to the table while that scan occurs.");
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }
}
