package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.AlterDomainStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterEnumStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterSeqStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterTableCmd;
import com.premiumminds.sonar.postgres.protobuf.AlterTableStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterTableType;
import com.premiumminds.sonar.postgres.protobuf.ClusterStmt;
import com.premiumminds.sonar.postgres.protobuf.ColumnDef;
import com.premiumminds.sonar.postgres.protobuf.Constraint;
import com.premiumminds.sonar.postgres.protobuf.CreateExtensionStmt;
import com.premiumminds.sonar.postgres.protobuf.CreateSchemaStmt;
import com.premiumminds.sonar.postgres.protobuf.CreateSeqStmt;
import com.premiumminds.sonar.postgres.protobuf.CreateStatsStmt;
import com.premiumminds.sonar.postgres.protobuf.CreateStmt;
import com.premiumminds.sonar.postgres.protobuf.CreateTableAsStmt;
import com.premiumminds.sonar.postgres.protobuf.DoStmt;
import com.premiumminds.sonar.postgres.protobuf.DropStmt;
import com.premiumminds.sonar.postgres.protobuf.DropdbStmt;
import com.premiumminds.sonar.postgres.protobuf.IndexStmt;
import com.premiumminds.sonar.postgres.protobuf.Node;
import com.premiumminds.sonar.postgres.protobuf.RawStmt;
import com.premiumminds.sonar.postgres.protobuf.RefreshMatViewStmt;
import com.premiumminds.sonar.postgres.protobuf.ReindexStmt;
import com.premiumminds.sonar.postgres.protobuf.RenameStmt;
import com.premiumminds.sonar.postgres.protobuf.VacuumStmt;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

public abstract class AbstractVisitorCheck implements VisitorCheck {

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
    public void visit(ReindexStmt reindexStmt) {

    }

    @Override
    public void visit(CreateTableAsStmt createTableAsStmt) {

    }

    @Override
    public void visit(CreateStmt createStmt) {
        createStmt.getTableEltsList()
            .forEach(tableElt -> {
                tableElt.getColumnDef()
                        .getConstraintsList()
                        .forEach(node -> visitCreateTableColumnConstraint(node.getConstraint()));

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
        columnDef.getConstraintsList()
                .forEach(node -> visitAlterTableColumnConstraint(node.getConstraint()));
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
    public void visit(VacuumStmt vacuumStmt) {

    }

    @Override
    public void visit(CreateExtensionStmt createExtensionStmt) {

    }

    @Override
    public void visit(RefreshMatViewStmt refreshMatViewStmt) {

    }

    @Override
    public void visit(DoStmt doStmt){

    }

    @Override
    public void visit(CreateStatsStmt createStatsStmt) {

    }

    @Override
    public void visit(ClusterStmt clusterStmt) {

    }

    @Override
    public void analyze(SensorContext context, InputFile file, TextRange textRange, RawStmt rawStmt) {
        this.context = context;
        this.file = file;
        this.textRange = textRange;

        final Node stmt = rawStmt.getStmt();
        if (stmt.hasCreateStmt()) {
            visit(stmt.getCreateStmt());
        } else if (stmt.hasIndexStmt()){
            visit(stmt.getIndexStmt());
        } else if (stmt.hasDropStmt()){
            visit(stmt.getDropStmt());
        } else if (stmt.hasDropdbStmt()){
            visit(stmt.getDropdbStmt());
        } else if (stmt.hasAlterTableStmt()){
            visit(stmt.getAlterTableStmt());
        } else if (stmt.hasRenameStmt()){
            visit(stmt.getRenameStmt());
        } else if (stmt.hasCreateSeqStmt()){
            visit(stmt.getCreateSeqStmt());
        } else if (stmt.hasAlterSeqStmt()){
            visit(stmt.getAlterSeqStmt());
        } else if (stmt.hasCreateSchemaStmt()){
            visit(stmt.getCreateSchemaStmt());
        } else if (stmt.hasAlterDomainStmt()) {
            visit(stmt.getAlterDomainStmt());
        } else if (stmt.hasCreateTableAsStmt()){
            visit(stmt.getCreateTableAsStmt());
        } else if (stmt.hasAlterEnumStmt()){
            visit(stmt.getAlterEnumStmt());
        } else if (stmt.hasReindexStmt()){
            visit(stmt.getReindexStmt());
        } else if (stmt.hasVacuumStmt()){
            visit(stmt.getVacuumStmt());
        } else if (stmt.hasCreateExtensionStmt()){
            visit(stmt.getCreateExtensionStmt());
        } else if (stmt.hasRefreshMatViewStmt()){
            visit(stmt.getRefreshMatViewStmt());
        } else if (stmt.hasDoStmt()){
            visit(stmt.getDoStmt());
        } else if (stmt.hasCreateStatsStmt()){
            visit(stmt.getCreateStatsStmt());
        } else if (stmt.hasClusterStmt()){
            visit(stmt.getClusterStmt());
        }
    }

    protected SensorContext getContext() {
        return context;
    }

    protected InputFile getFile() {
        return file;
    }

    protected TextRange getTextRange() {
        return textRange;
    }

    protected abstract RuleKey getRule();

    protected void newIssue(String message) {
        NewIssue newIssue = getContext().newIssue()
                .forRule(getRule());
        NewIssueLocation primaryLocation = newIssue.newLocation()
                .on(getFile())
                .at(getTextRange())
                .message(message);
        newIssue.at(primaryLocation);
        newIssue.save();
    }
}
