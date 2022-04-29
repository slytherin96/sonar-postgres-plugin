package com.premiumminds.sonar.postgres.visitors;

import com.premiumminds.sonar.postgres.protobuf.AlterDomainStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterEnumStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterSeqStmt;
import com.premiumminds.sonar.postgres.protobuf.AlterTableCmd;
import com.premiumminds.sonar.postgres.protobuf.AlterTableStmt;
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

public interface VisitorCheck {

    void visit(AlterSeqStmt alterSeqStmt);
    void visit(AlterTableStmt alterTableStmt);
    void visit(DropStmt dropStmt);
    void visit(CreateSeqStmt createSeqStmt);
    void visit(RenameStmt renameStmt);
    void visit(DropdbStmt dropdbStmt);
    void visit(IndexStmt indexStmt);
    void visit(CreateTableAsStmt createTableAsStmt);
    void visit(CreateStmt createStmt);
    void visit(CreateSchemaStmt createSchemaStmt);
    void visit(AlterDomainStmt alterDomainStmt);
    void visit(AlterEnumStmt alterEnumStmt);
    void visit(AlterTableCmd alterTableCmd);
    void visitCreateTableColumnConstraint(Constraint constraint);
    void visitCreateTableTableConstraint(Constraint constraint);
    void visitAlterTableColumnConstraint(Constraint constraint);
    void visitAlterTableTableConstraint(Constraint constraint);
    void visit(ColumnDef columnDef);

    void analyze(SensorContext context, InputFile file, TextRange textRange, RawStmt rawStmt);

}
