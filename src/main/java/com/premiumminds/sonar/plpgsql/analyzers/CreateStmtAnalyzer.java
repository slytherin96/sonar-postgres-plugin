package com.premiumminds.sonar.plpgsql.analyzers;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import com.premiumminds.sonar.postgres.protobuf.ConstrType;
import com.premiumminds.sonar.postgres.protobuf.Constraint;
import com.premiumminds.sonar.postgres.protobuf.CreateStmt;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

public class CreateStmtAnalyzer implements Analyzer {

    private final CreateStmt createStmt;

    public CreateStmtAnalyzer(CreateStmt createStmt) {

        this.createStmt = createStmt;
    }

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange) {

        if (!createStmt.getIfNotExists()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF NOT EXISTS to CREATE TABLE " + createStmt.getRelation().getRelname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        createStmt.getTableEltsList()
                .forEach(tableElt -> {
                    tableElt.getColumnDef().getConstraintsList().forEach(node -> {
                        ConstraintAnalyzer constraintAnalyzer;
                        final Constraint constraint = node.getConstraint();
                        final ConstrType contype = constraint.getContype();
                        switch (contype){
                            case CONSTR_FOREIGN:
                                constraintAnalyzer = new ForeignKeyConstraintAnalyzer();
                                break;
                            default:
                                return;
                        }
                        constraintAnalyzer.validate(context, file, textRange, constraint);
                    });

                    final TypeNamesAnalyzer typeNamesAnalyzer = new TypeNamesAnalyzer();
                    typeNamesAnalyzer.validate(context, file, textRange, tableElt.getColumnDef());

                    final Constraint constraint = tableElt.getConstraint();
                    ConstraintAnalyzer constraintAnalyzer;
                    final ConstrType contype = constraint.getContype();
                    switch (contype){
                        case CONSTR_FOREIGN:
                            constraintAnalyzer = new ForeignKeyConstraintAnalyzer();
                            break;
                        default:
                            return;
                    }
                    constraintAnalyzer.validate(context, file, textRange, constraint);
                });

    }
}
