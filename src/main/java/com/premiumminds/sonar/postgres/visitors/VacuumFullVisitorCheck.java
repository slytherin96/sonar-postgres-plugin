package com.premiumminds.sonar.postgres.visitors;

import java.util.Optional;

import com.premiumminds.sonar.postgres.protobuf.Node;
import com.premiumminds.sonar.postgres.protobuf.VacuumStmt;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_CONCURRENTLY;
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
            NewIssue newIssue = getContext().newIssue()
                    .forRule(RULE_VACUUM_FULL);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("VACUUM FULL exclusively locks the table while running");
            newIssue.at(primaryLocation);
            newIssue.save();
        }
        super.visit(vacuumStmt);
    }
}
