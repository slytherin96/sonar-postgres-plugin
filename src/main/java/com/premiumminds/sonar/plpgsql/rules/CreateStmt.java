package com.premiumminds.sonar.plpgsql.rules;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

public class CreateStmt implements Stmt {

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange, JsonObject jsonObject) {
        final JsonObject relation = jsonObject.getJsonObject("relation");
        final String relname = relation.getString("relname");

        if(!jsonObject.getBoolean("if_not_exists", false)){

            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF NOT EXISTS to CREATE TABLE " + relname);
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        final JsonArray tableElts = jsonObject.getJsonArray("tableElts");
        if (tableElts != null){
            tableElts.forEach(tableElt -> {
                final JsonObject columnDef = tableElt.asJsonObject().getJsonObject("ColumnDef");
                if (columnDef != null) {
                    final JsonArray constraints = columnDef.getJsonArray("constraints");
                    if (constraints != null){
                        constraints.forEach(c -> {
                            final JsonObject constraintJson = c.asJsonObject()
                                    .getJsonObject("Constraint");
                            final EnumConstraint contype = EnumConstraint.valueOf(constraintJson.getString("contype"));
                            Constraint constraint;
                            switch (contype){
                                case CONSTR_FOREIGN:
                                    constraint = new ForeignKeyConstraint();
                                    break;
                                default:
                                    return;
                            }
                            constraint.validate(context, file, textRange, constraintJson);
                        });
                    }
                    final TypeNames typeNames = new TypeNames();
                    typeNames.validate(context, file, textRange, columnDef);
                }
                final JsonObject constraintJson = tableElt.asJsonObject().getJsonObject("Constraint");
                if (constraintJson != null){
                    final EnumConstraint contype = EnumConstraint.valueOf(constraintJson.getString("contype"));
                    Constraint constraint;
                    switch (contype){
                        case CONSTR_FOREIGN:
                            constraint = new ForeignKeyConstraint();
                            break;
                        default:
                            return;
                    }
                    constraint.validate(context, file, textRange, constraintJson);
                }
            });
        }


    }
}
