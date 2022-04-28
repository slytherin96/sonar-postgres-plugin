package com.premiumminds.sonar.plpgsql.analyzers;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import com.premiumminds.sonar.plpgsql.protobuf.AlterTableCmd;
import com.premiumminds.sonar.plpgsql.protobuf.AlterTableStmt;
import com.premiumminds.sonar.plpgsql.protobuf.AlterTableType;
import com.premiumminds.sonar.plpgsql.protobuf.ColumnDef;
import com.premiumminds.sonar.plpgsql.protobuf.ConstrType;
import com.premiumminds.sonar.plpgsql.protobuf.Constraint;
import com.premiumminds.sonar.plpgsql.protobuf.ObjectType;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_SETTING_NOT_NULLABLE_FIELD;

public class AlterTableStmtAnalyzer implements Analyzer {
    private final AlterTableStmt alterTableStmt;

    public AlterTableStmtAnalyzer(AlterTableStmt alterTableStmt) {
        this.alterTableStmt = alterTableStmt;
    }

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange) {

        if(!alterTableStmt.getMissingOk()){
            String message;
            final ObjectType relkind = alterTableStmt.getRelkind();
            switch (relkind){
                case OBJECT_TABLE:
                    message = "Add IF EXISTS to ALTER TABLE " + alterTableStmt.getRelation().getRelname();
                    break;
                case OBJECT_INDEX:
                    message = "Add IF EXISTS to ALTER INDEX " + alterTableStmt.getRelation().getRelname();
                    break;
                default:
                    message = "Add IF EXISTS";
                    break;
            }

            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message(message);
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        alterTableStmt.getCmdsList().forEach(node -> {
            final AlterTableCmd alterTableCmd = node.getAlterTableCmd();
            final AlterTableType subtype = alterTableCmd.getSubtype();
            switch (subtype){
                case AT_SetNotNull:
                    setNotNull(context, file, textRange, alterTableCmd);
                    break;
                case AT_AddConstraint:
                    addConstraint(context, file, textRange, alterTableCmd);
                    break;
                case AT_AlterColumnType:
                    alterColumnType(context, file, textRange, alterTableCmd);
                    break;
                case AT_DropConstraint:
                    dropConstraint(context, file, textRange, alterTableCmd);
                    break;
                case AT_DropColumn:
                    dropColumn(context, file, textRange, alterTableCmd);
                    break;
                case AT_AddColumn:
                    addColumn(context, file, textRange, alterTableCmd);
                    break;
            }
        });
    }

    private void addColumn(SensorContext context, InputFile file, TextRange textRange, AlterTableCmd alterTableCmd) {

        final ColumnDef columnDef = alterTableCmd.getDef().getColumnDef();
        final String colname = columnDef.getColname();

        if (!alterTableCmd.getMissingOk()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF NOT EXISTS to ADD COLUMN " + colname);
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        final TypeNamesAnalyzer typeNamesAnalyzer = new TypeNamesAnalyzer();
        typeNamesAnalyzer.validate(context, file, textRange, columnDef);

        columnDef.getConstraintsList().forEach(node -> {
            ConstraintAnalyzer constraintAnalyzer;
            final Constraint constraint = node.getConstraint();
            final ConstrType contype = constraint.getContype();
            switch (contype){
                case CONSTR_DEFAULT:
                    constraintAnalyzer = new DefaultConstraintAnalyzer();
                    break;
                case CONSTR_FOREIGN:
                    constraintAnalyzer = new ForeignKeyConstraintAnalyzer();
                    break;
                default:
                    return;
            }
            constraintAnalyzer.validate(context, file, textRange, constraint);
        });
    }

    private void dropColumn(SensorContext context, InputFile file, TextRange textRange, AlterTableCmd alterTableCmd) {
        final String name = alterTableCmd.getName();

        if(!alterTableCmd.getMissingOk()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF EXISTS to DROP COLUMN " + name);
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }

    private void dropConstraint(SensorContext context, InputFile file, TextRange textRange, AlterTableCmd alterTableCmd) {
        final String name = alterTableCmd.getName();

        if(!alterTableCmd.getMissingOk()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF EXISTS to DROP CONSTRAINT " + name);
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }

    private void alterColumnType(SensorContext context, InputFile file, TextRange textRange, AlterTableCmd alterTableCmd) {
        NewIssue newIssue = context.newIssue()
                .forRule(PlPgSqlRulesDefinition.RULE_CHANGING_COLUMN_TYPE);
        NewIssueLocation primaryLocation = newIssue.newLocation()
                .on(file)
                .at(textRange)
                .message("Changing a column type requires an ACCESS EXCLUSIVE lock on the table which blocks reads and writes while the table is rewritten.");
        newIssue.at(primaryLocation);
        newIssue.save();
    }

    private void addConstraint(SensorContext context, InputFile file, TextRange textRange, AlterTableCmd alterTableCmd) {

        ConstraintAnalyzer constraintAnalyzer;
        final Constraint constraint = alterTableCmd.getDef().getConstraint();
        final ConstrType contype = constraint.getContype();
        switch (contype){
            case CONSTR_UNIQUE:
                constraintAnalyzer = new UniqueConstraintAnalyzer();
                break;
            case CONSTR_PRIMARY:
                constraintAnalyzer = new PrimaryKeyConstraintAnalyzer();
                break;
            case CONSTR_FOREIGN:
                constraintAnalyzer = new ForeignKeyConstraintAnalyzer();
                break;
            case CONSTR_CHECK:
                constraintAnalyzer = new CheckContraintAnalyzer();
                break;
            default:
                return;
        }
        constraintAnalyzer.validate(context, file, textRange, constraint);
    }

    private void setNotNull(SensorContext context, InputFile file, TextRange textRange, AlterTableCmd alterTableCmd) {
        NewIssue newIssue = context.newIssue()
                .forRule(RULE_SETTING_NOT_NULLABLE_FIELD);
        NewIssueLocation primaryLocation = newIssue.newLocation()
                .on(file)
                .at(textRange)
                .message("Setting a column as NOT NULL will require a scan of the entire table. However, if a valid CHECK constraint is found which proves no NULL can exist, then the table scan is skipped.");
        newIssue.at(primaryLocation);
        newIssue.save();
    }
}
