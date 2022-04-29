package com.premiumminds.sonar.postgres.visitors;

import java.util.List;
import java.util.stream.Collectors;

import com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition;
import com.premiumminds.sonar.postgres.protobuf.AlterDomainStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterEnumStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterSeqStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterTableCmd;
import com.premiumminds.sonar.postgres.protobuf.AlterTableStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterTableType;
import com.premiumminds.sonar.postgres.protobuf.ColumnDef;
import com.premiumminds.sonar.postgres.protobuf.CreateSchemaStmt;
import com.premiumminds.sonar.postgres.protobuf.CreateSeqStmt;
import com.premiumminds.sonar.postgres.protobuf.CreateStmt;
import com.premiumminds.sonar.postgres.protobuf.CreateTableAsStmt;
import com.premiumminds.sonar.postgres.protobuf.DropStmt;
import com.premiumminds.sonar.postgres.protobuf.IndexStmt;
import com.premiumminds.sonar.postgres.protobuf.ObjectType;
import com.premiumminds.sonar.postgres.protobuf.RenameStmt;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.check.Rule;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS;
import static com.premiumminds.sonar.postgres.protobuf.ObjectType.OBJECT_INDEX;
import static com.premiumminds.sonar.postgres.protobuf.ObjectType.OBJECT_MATVIEW;
import static com.premiumminds.sonar.postgres.protobuf.ObjectType.OBJECT_VIEW;

@Rule(key = "prefer-robust-stmts")
public class RobustStatementsVisitorCheck extends AbstractVisitorCheck {

    @Override
    public void visit(DropStmt dropStmt) {

        final List<String> names = dropStmt.getObjectsList()
                .stream()
                .map(y -> y.getList()
                        .getItemsList())
                .flatMap(List::stream)
                .map(x -> x.getString().getStr())
                .collect(Collectors.toList());

        final List<String> schemas = dropStmt.getObjectsList()
                .stream()
                .map(y -> y.getString().getStr())
                .collect(Collectors.toList());

        final List<String> domains = dropStmt.getObjectsList()
                .stream()
                .map(y -> y.getTypeName()
                        .getNamesList())
                .flatMap(List::stream)
                .map(x -> x.getString().getStr())
                .collect(Collectors.toList());

        if(!dropStmt.getMissingOk()){
            String message;
            final ObjectType removeType = dropStmt.getRemoveType();
            switch (removeType){
                case OBJECT_TABLE:
                    message = "Add IF EXISTS to DROP TABLE " + String.join(", ", names);
                    break;
                case OBJECT_SEQUENCE:
                    message = "Add IF EXISTS to DROP SEQUENCE " + String.join(", ", names);
                    break;
                case OBJECT_INDEX:
                    message = "Add IF EXISTS to DROP INDEX " + String.join(", ", names);
                    break;
                case OBJECT_VIEW:
                    message = "Add IF EXISTS to DROP VIEW " + String.join(", ", names);
                    break;
                case OBJECT_SCHEMA:
                    message = "Add IF EXISTS to DROP SCHEMA " + String.join(", ", schemas);
                    break;
                case OBJECT_DOMAIN:
                    message = "Add IF EXISTS to DROP DOMAIN " + String.join(", ", domains);
                    break;
                case OBJECT_MATVIEW:
                    message = "Add IF EXISTS to DROP MATERIALIZED VIEW " + String.join(", ", names);
                    break;
                case OBJECT_TYPE:
                    message = "Add IF EXISTS to DROP TYPE " + String.join(", ", domains);
                    break;
                default:
                    message = "Add IF EXISTS";
                    break;
            }

            NewIssue newIssue = getContext().newIssue()
                    .forRule(RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message(message);
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        super.visit(dropStmt);
    }

    @Override
    public void visit(RenameStmt renameStmt) {
        final ObjectType renameType = renameStmt.getRenameType();
        if (renameType.equals(OBJECT_INDEX) && !renameStmt.getMissingOk()){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Add IF EXISTS to ALTER INDEX " + renameStmt.getRelation().getRelname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        if (renameType.equals(OBJECT_VIEW) && !renameStmt.getMissingOk()){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Add IF EXISTS to ALTER VIEW " + renameStmt.getRelation().getRelname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        if (renameType.equals(OBJECT_MATVIEW) && !renameStmt.getMissingOk()){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Add IF EXISTS to ALTER MATERIALIZED VIEW " + renameStmt.getRelation().getRelname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        super.visit(renameStmt);
    }

    @Override
    public void visit(IndexStmt indexStmt) {
        if (!indexStmt.getIfNotExists()){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Add IF NOT EXISTS to CREATE INDEX " + indexStmt.getIdxname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        super.visit(indexStmt);
    }

    @Override
    public void visit(AlterSeqStmt alterSeqStmt) {
        if (!alterSeqStmt.getMissingOk()){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Add IF EXISTS to ALTER SEQUENCE " + alterSeqStmt.getSequence().getRelname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        super.visit(alterSeqStmt);
    }

    @Override
    public void visit(CreateSeqStmt createSeqStmt) {
        if (!createSeqStmt.getIfNotExists()){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Add IF NOT EXISTS to CREATE SEQUENCE " + createSeqStmt.getSequence().getRelname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }
        super.visit(createSeqStmt);
    }

    @Override
    public void visit(CreateTableAsStmt createTableAsStmt) {
        String message = null;
        final ObjectType relkind = createTableAsStmt.getRelkind();
        switch (relkind){
            case OBJECT_TABLE:
                message = "Add IF NOT EXISTS to CREATE TABLE " + createTableAsStmt.getInto().getRel().getRelname() + " AS ";
                break;
            case OBJECT_MATVIEW:
                message = "Add IF NOT EXISTS to CREATE MATERIALIZED VIEW " + createTableAsStmt.getInto().getRel().getRelname();
                break;
        }
        if (!createTableAsStmt.getIfNotExists()){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message(message);
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        super.visit(createTableAsStmt);
    }

    @Override
    public void visit(CreateStmt createStmt) {
        if (!createStmt.getIfNotExists()){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Add IF NOT EXISTS to CREATE TABLE " + createStmt.getRelation().getRelname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }
        super.visit(createStmt);
    }

    @Override
    public void visit(CreateSchemaStmt createSchemaStmt) {
        if (!createSchemaStmt.getIfNotExists()){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Add IF NOT EXISTS to CREATE SCHEMA " + createSchemaStmt.getSchemaname());
            newIssue.at(primaryLocation);
            newIssue.save();
        }
        super.visit(createSchemaStmt);
    }

    @Override
    public void visit(AlterDomainStmt alterDomainStmt) {
        if (!alterDomainStmt.getMissingOk()){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Add IF EXISTS to DROP CONSTRAINT " + alterDomainStmt.getName());
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }

    @Override
    public void visit(AlterEnumStmt alterEnumStmt) {
        if (!alterEnumStmt.getSkipIfNewValExists()){
            NewIssue newIssue = getContext().newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message("Add IF NOT EXISTS to ADD VALUE " + alterEnumStmt.getNewVal());
            newIssue.at(primaryLocation);
            newIssue.save();
        }
        System.out.println(alterEnumStmt);
    }

    @Override
    public void visit(AlterTableStmt alterTableStmt) {
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

            NewIssue newIssue = getContext().newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(getFile())
                    .at(getTextRange())
                    .message(message);
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        super.visit(alterTableStmt);
    }

    @Override
    public void visit(AlterTableCmd alterTableCmd) {
        final AlterTableType subtype = alterTableCmd.getSubtype();
        switch (subtype){
            case AT_DropConstraint:
                dropConstraint(getContext(), getFile(), getTextRange(), alterTableCmd);
                break;
            case AT_DropColumn:
                dropColumn(getContext(), getFile(), getTextRange(), alterTableCmd);
                break;
            case AT_AddColumn:
                addColumn(getContext(), getFile(), getTextRange(), alterTableCmd);
                break;
        }

        super.visit(alterTableCmd);
    }

    private void dropConstraint(SensorContext context, InputFile file, TextRange textRange, AlterTableCmd alterTableCmd) {
        final String name = alterTableCmd.getName();

        if(!alterTableCmd.getMissingOk()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF EXISTS to DROP CONSTRAINT " + name);
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }

    private void dropColumn(SensorContext context, InputFile file, TextRange textRange, AlterTableCmd alterTableCmd) {
        final String name = alterTableCmd.getName();

        if(!alterTableCmd.getMissingOk()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF EXISTS to DROP COLUMN " + name);
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }

    private void addColumn(SensorContext context, InputFile file, TextRange textRange, AlterTableCmd alterTableCmd) {
        final ColumnDef columnDef = alterTableCmd.getDef().getColumnDef();
        final String colname = columnDef.getColname();

        if (!alterTableCmd.getMissingOk()){
            NewIssue newIssue = context.newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF NOT EXISTS to ADD COLUMN " + colname);
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }
}
