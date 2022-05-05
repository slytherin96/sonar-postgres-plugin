package com.premiumminds.sonar.postgres.visitors;

import java.util.Optional;

import com.premiumminds.sonar.postgres.protobuf.Node;
import com.premiumminds.sonar.postgres.protobuf.VacuumStmt;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_VACUUM_FULL;

@Rule(key = "vacuum-full")
public class VacuumFullVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(VacuumStmt vacuumStmt) {
        final Optional<Node> full = vacuumStmt.getOptionsList()
                .stream()
                .filter(x -> "full".equals(x.getDefElem().getDefname()))
                .findFirst();
        if (full.isPresent()){
            newIssue("VACUUM FULL exclusively locks the table while running");
        }
        super.visit(vacuumStmt);
    }

    @Override
    protected RuleKey getRule() {
        return RULE_VACUUM_FULL;
    }
}
