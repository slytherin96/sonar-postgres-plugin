package com.premiumminds.sonar.plpgsql.rules;

import com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_SETTING_NOT_NULLABLE_FIELD;

public class AlterTableStmt implements Stmt {
    private static final Logger LOGGER = Loggers.get(AlterTableStmt.class);

    private enum SubType {
        AT_SetNotNull,
        AT_AddConstraint,
        AT_ValidateConstraint,
        AT_AlterColumnType,
        AT_DropConstraint,
        AT_DropColumn,
        AT_AddColumn,
        ;
    }

    @Override
    public void validate(SensorContext context, InputFile file, TextRange textRange, JsonObject jsonObject) {
        final JsonObject relation = jsonObject.getJsonObject("relation");
        final String relname = relation.getString("relname");
        final JsonArray cmds = jsonObject.getJsonArray("cmds");
        cmds.forEach(x -> {
            final JsonObject cmd = x.asJsonObject();
            final JsonObject alterTableCmd = cmd.getJsonObject("AlterTableCmd");

            final SubType subtype = SubType.valueOf(alterTableCmd.getString("subtype"));
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
        if(!jsonObject.getBoolean("missing_ok", false)){
            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_IF_EXISTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF EXISTS to ALTER TABLE " + relname);
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }

    private void alterColumnType(SensorContext context, InputFile file, TextRange textRange, JsonObject alterTableCmd) {
        NewIssue newIssue = context.newIssue()
                .forRule(PlPgSqlRulesDefinition.RULE_CHANGING_COLUMN_TYPE);
        NewIssueLocation primaryLocation = newIssue.newLocation()
                .on(file)
                .at(textRange)
                .message("Changing a column type requires an ACCESS EXCLUSIVE lock on the table which blocks reads and writes while the table is rewritten.");
        newIssue.at(primaryLocation);
        newIssue.save();
    }

    private void dropConstraint(SensorContext context, InputFile file, TextRange textRange, JsonObject alterTableCmd) {
        final String name = alterTableCmd.getString("name");

        if(!alterTableCmd.getBoolean("missing_ok", false)){
            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_IF_EXISTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF EXISTS to DROP CONSTRAINT " + name);
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }

    private void setNotNull(SensorContext context, InputFile file, TextRange textRange, JsonObject alterTableCmd) {
        NewIssue newIssue = context.newIssue()
                .forRule(RULE_SETTING_NOT_NULLABLE_FIELD);
        NewIssueLocation primaryLocation = newIssue.newLocation()
                .on(file)
                .at(textRange)
                .message("Ordinarily this is checked during the ALTER TABLE by scanning the entire table;");
        newIssue.at(primaryLocation);
        newIssue.save();
    }

    private void addConstraint(SensorContext context, InputFile file, TextRange textRange, JsonObject alterTableCmd) {
        final JsonObject constraintJson = alterTableCmd.getJsonObject("def").getJsonObject("Constraint");
        final EnumConstraint contype = EnumConstraint.valueOf(constraintJson.getString("contype"));
        Constraint constraint;
        switch (contype){
            case CONSTR_UNIQUE:
                constraint = new UniqueConstraint();
                break;
            case CONSTR_PRIMARY:
                constraint = new PrimaryKeyConstraint();
                break;
            case CONSTR_FOREIGN:
                constraint = new ForeignKeyConstraint();
                break;
            case CONSTR_CHECK:
                constraint = new CheckContraint();
                break;
            default:
                return;
        }
        constraint.validate(context, file, textRange, constraintJson);

    }

    private void addColumn(SensorContext context, InputFile file, TextRange textRange, JsonObject alterTableCmd) {
        final JsonObject columnDef = alterTableCmd
                .getJsonObject("def")
                .getJsonObject("ColumnDef");
        final String colname = columnDef.getString("colname");

        if(!alterTableCmd.getBoolean("missing_ok", false)){
            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_IF_NOT_EXISTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF NOT EXISTS to ADD COLUMN " + colname);
            newIssue.at(primaryLocation);
            newIssue.save();
        }

        final JsonObject typeName = columnDef.getJsonObject("typeName");
        final JsonArray names = typeName.getJsonArray("names");
        names.forEach(x -> {
            final JsonString str = x.asJsonObject().getJsonObject("String").getJsonString("str");
            if (str != null && "bpchar".equals(str.getString())){
                NewIssue newIssue = context.newIssue()
                        .forRule(PlPgSqlRulesDefinition.RULE_BAN_CHAR_FIELD);
                NewIssueLocation primaryLocation = newIssue.newLocation()
                        .on(file)
                        .at(textRange)
                        .message("Using character is likely a mistake and should almost always be replaced by text or varchar.");
                newIssue.at(primaryLocation);
                newIssue.save();
            }
            if (str != null && "varchar".equals(str.getString()) && typeName.containsKey("typmods")){
                NewIssue newIssue = context.newIssue()
                        .forRule(PlPgSqlRulesDefinition.RULE_PREFER_TEXT_FIELD);
                NewIssueLocation primaryLocation = newIssue.newLocation()
                        .on(file)
                        .at(textRange)
                        .message("Changing the size of a varchar field requires an ACCESS EXCLUSIVE lock, that will prevent all reads and writes to the table.");
                newIssue.at(primaryLocation);
                newIssue.save();
            }
        });

        final JsonArray constraints = columnDef.getJsonArray("constraints");
        if (constraints != null){
            constraints.forEach(c -> {
                final JsonObject constraintJson = c.asJsonObject()
                        .getJsonObject("Constraint");
                final EnumConstraint contype = EnumConstraint.valueOf(constraintJson.getString("contype"));
                Constraint constraint;
                switch (contype){
                    case CONSTR_DEFAULT:
                        constraint = new DefaultConstraint();
                        break;
                    case CONSTR_FOREIGN:
                        constraint = new ForeignKeyConstraint();
                        break;
                    default:
                        return;
                }
                constraint.validate(context, file, textRange, constraintJson);
            });
        }
    }

    private void dropColumn(SensorContext context, InputFile file, TextRange textRange, JsonObject alterTableCmd) {
        final String name = alterTableCmd.getString("name");

        if(!alterTableCmd.getBoolean("missing_ok", false)){
            NewIssue newIssue = context.newIssue()
                    .forRule(PlPgSqlRulesDefinition.RULE_IF_EXISTS);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(textRange)
                    .message("Add IF EXISTS to DROP COLUMN " + name);
            newIssue.at(primaryLocation);
            newIssue.save();
        }
    }
}
