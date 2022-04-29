package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.AlterDomainStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterEnumStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterSeqStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterTableCmd;
import com.premiumminds.sonar.postgres.protobuf.AlterTableStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterTableType;
import com.premiumminds.sonar.postgres.protobuf.ColumnDef;
import com.premiumminds.sonar.postgres.protobuf.Constraint;
import com.premiumminds.sonar.postgres.protobuf.CreateSchemaStmt;
import com.premiumminds.sonar.postgres.protobuf.CreateSeqStmt;
import com.premiumminds.sonar.postgres.protobuf.CreateStmt;
import com.premiumminds.sonar.postgres.protobuf.CreateTableAsStmt;
import com.premiumminds.sonar.postgres.protobuf.DropStmt;
import com.premiumminds.sonar.postgres.protobuf.DropdbStmt;
import com.premiumminds.sonar.postgres.protobuf.IndexStmt;
import com.premiumminds.sonar.postgres.protobuf.RawStmt;
import com.premiumminds.sonar.postgres.protobuf.RenameStmt;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;

public class AbstractVisitorCheck implements VisitorCheck {

    private SensorContext context;
    private InputFile file;
    private TextRange textRange;

    @Override
    public void visit(AlterSeqStmt alterSeqStmt) {

    }

    @Override
    public void visit(AlterTableStmt alterTableStmt) {
        alterTableStmt.getCmdsList().forEach(node -> visit(node.getAlterTableCmd()));
    }

    @Override
    public void visit(DropStmt dropStmt) {

    }

    @Override
    public void visit(CreateSeqStmt createSeqStmt) {

    }

    @Override
    public void visit(RenameStmt renameStmt) {

    }

    @Override
    public void visit(DropdbStmt dropdbStmt) {

    }

    @Override
    public void visit(IndexStmt indexStmt) {

    }

    @Override
    public void visit(CreateTableAsStmt createTableAsStmt) {

    }

    @Override
    public void visit(CreateStmt createStmt) {
        createStmt.getTableEltsList()
            .forEach(tableElt -> {
                tableElt.getColumnDef().getConstraintsList().forEach(node -> {
                    visitCreateTableColumnConstraint(node.getConstraint());
                });

                visitCreateTableTableConstraint(tableElt.getConstraint());

                visit(tableElt.getColumnDef());
            });
    }


    @Override
    public void visit(CreateSchemaStmt createSchemaStmt) {

    }

    @Override
    public void visit(AlterDomainStmt alterDomainStmt) {

    }

    @Override
    public void visit(AlterEnumStmt alterEnumStmt) {

    }

    @Override
    public void visit(AlterTableCmd alterTableCmd) {

        final AlterTableType subtype = alterTableCmd.getSubtype();
        if (AlterTableType.AT_AddConstraint.equals(subtype)){
            visitAlterTableTableConstraint(alterTableCmd.getDef().getConstraint());
        }
        if (AlterTableType.AT_AddColumn.equals(subtype)){
            visit(alterTableCmd.getDef().getColumnDef());
        }

        final ColumnDef columnDef = alterTableCmd.getDef().getColumnDef();
        columnDef.getConstraintsList().forEach(node -> {
            visitAlterTableColumnConstraint(node.getConstraint());
        });
    }

    @Override
    public void visitCreateTableColumnConstraint(Constraint constraint) {

    }

    @Override
    public void visitCreateTableTableConstraint(Constraint constraint) {

    }

    @Override
    public void visitAlterTableColumnConstraint(Constraint constraint) {

    }

    @Override
    public void visitAlterTableTableConstraint(Constraint constraint) {

    }

    @Override
    public void visit(ColumnDef columnDef) {

    }

    @Override
    public void analyze(SensorContext context, InputFile file, TextRange textRange, RawStmt rawStmt) {
        this.context = context;
        this.file = file;
        this.textRange = textRange;

        if (rawStmt.getStmt().hasCreateStmt()) {
            visit(rawStmt.getStmt().getCreateStmt());
        } else if (rawStmt.getStmt().hasIndexStmt()){
            visit(rawStmt.getStmt().getIndexStmt());
        } else if (rawStmt.getStmt().hasDropStmt()){
            visit(rawStmt.getStmt().getDropStmt());
        } else if (rawStmt.getStmt().hasDropdbStmt()){
            visit(rawStmt.getStmt().getDropdbStmt());
        } else if (rawStmt.getStmt().hasAlterTableStmt()){
            visit(rawStmt.getStmt().getAlterTableStmt());
        } else if (rawStmt.getStmt().hasRenameStmt()){
            visit(rawStmt.getStmt().getRenameStmt());
        } else if (rawStmt.getStmt().hasCreateSeqStmt()){
            visit(rawStmt.getStmt().getCreateSeqStmt());
        } else if (rawStmt.getStmt().hasAlterSeqStmt()){
            visit(rawStmt.getStmt().getAlterSeqStmt());
        } else if (rawStmt.getStmt().hasCreateSchemaStmt()){
            visit(rawStmt.getStmt().getCreateSchemaStmt());
        } else if (rawStmt.getStmt().hasAlterDomainStmt()) {
            visit(rawStmt.getStmt().getAlterDomainStmt());
        } else if (rawStmt.getStmt().hasCreateTableAsStmt()){
            visit(rawStmt.getStmt().getCreateTableAsStmt());
        } else if (rawStmt.getStmt().hasAlterEnumStmt()){
            visit(rawStmt.getStmt().getAlterEnumStmt());
        }
    }

    public SensorContext getContext() {
        return context;
    }

    public InputFile getFile() {
        return file;
    }

    public TextRange getTextRange() {
        return textRange;
    }
}
