package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition;
import com.premiumminds.sonar.postgres.protobuf.ColumnDef;
import com.premiumminds.sonar.postgres.protobuf.TypeName;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_BAN_CHAR_FIELD;

@Rule(key = "ban-char-field")
public class BanCharFieldVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(ColumnDef columnDef) {

        final TypeName typeName = columnDef.getTypeName();
        typeName.getNamesList().forEach(name -> {
            final String str = name.getString().getStr();
            if ("bpchar".equals(str)){
                NewIssue newIssue = getContext().newIssue()
                        .forRule(RULE_BAN_CHAR_FIELD);
                NewIssueLocation primaryLocation = newIssue.newLocation()
                        .on(getFile())
                        .at(getTextRange())
                        .message("Using character is likely a mistake and should almost always be replaced by text or varchar.");
                newIssue.at(primaryLocation);
                newIssue.save();
            }
        });

        super.visit(columnDef);
    }
}
