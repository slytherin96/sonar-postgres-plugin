package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.ColumnDef;
import com.premiumminds.sonar.postgres.protobuf.TypeName;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_PREFER_IDENTITY_FIELD;

@Rule(key = "prefer-identity-field")
public class PreferIdentityVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(ColumnDef columnDef) {
        final TypeName typeName = columnDef.getTypeName();
        typeName.getNamesList().forEach(name -> {
            final String str = name.getString().getStr();

            if ("serial".equals(str)){
                NewIssue newIssue = getContext().newIssue()
                        .forRule(RULE_PREFER_IDENTITY_FIELD);
                NewIssueLocation primaryLocation = newIssue.newLocation()
                        .on(getFile())
                        .at(getTextRange())
                        .message("For new applications, identity columns should be used instead.");
                newIssue.at(primaryLocation);
                newIssue.save();
            }
        });

        super.visit(columnDef);
    }
}
