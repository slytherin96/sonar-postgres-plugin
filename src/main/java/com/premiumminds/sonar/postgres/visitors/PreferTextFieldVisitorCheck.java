package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition;
import com.premiumminds.sonar.postgres.protobuf.ColumnDef;
import com.premiumminds.sonar.postgres.protobuf.TypeName;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_PREFER_TEXT_FIELD;

@Rule(key = "prefer-text-field")
public class PreferTextFieldVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(ColumnDef columnDef) {
        final TypeName typeName = columnDef.getTypeName();
        typeName.getNamesList().forEach(name -> {
            final String str = name.getString().getStr();

            if ("varchar".equals(str) && typeName.getTypmodsList().size() != 0){
                NewIssue newIssue = getContext().newIssue()
                        .forRule(RULE_PREFER_TEXT_FIELD);
                NewIssueLocation primaryLocation = newIssue.newLocation()
                        .on(getFile())
                        .at(getTextRange())
                        .message("Changing the size of a varchar field requires an ACCESS EXCLUSIVE lock, that will prevent all reads and writes to the table.");
                newIssue.at(primaryLocation);
                newIssue.save();
            }
        });

        super.visit(columnDef);
    }
}
