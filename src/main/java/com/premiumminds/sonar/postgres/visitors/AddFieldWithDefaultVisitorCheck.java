package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.ConstrType;
import com.premiumminds.sonar.postgres.protobuf.Constraint;
import com.premiumminds.sonar.postgres.protobuf.Node;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_ADD_FIELD_WITH_DEFAULT;
import static com.premiumminds.sonar.postgres.protobuf.ConstrType.CONSTR_DEFAULT;

@Rule(key = "adding-field-with-default")
public class AddFieldWithDefaultVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visitAlterTableColumnConstraint(Constraint constraint) {
        final ConstrType contype = constraint.getContype();
        if (CONSTR_DEFAULT.equals(contype)){
            final Node rawExpr = constraint.getRawExpr();
            if (rawExpr.hasFuncCall()){
                newIssue("Adding a field with a VOLATILE default can cause table rewrites, which will take an ACCESS EXCLUSIVE lock on the table, blocking reads / writes while the statement is running.");
            }
        }

        super.visitAlterTableColumnConstraint(constraint);
    }

    @Override
    protected RuleKey getRule() {
        return RULE_ADD_FIELD_WITH_DEFAULT;
    }
}
