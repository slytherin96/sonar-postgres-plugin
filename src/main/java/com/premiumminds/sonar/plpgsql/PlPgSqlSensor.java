package com.premiumminds.sonar.plpgsql;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.premiumminds.sonar.plpgsql.libpg_query.PGQueryLibrary;
import com.premiumminds.sonar.plpgsql.libpg_query.PgQueryParseResult;
import com.premiumminds.sonar.plpgsql.rules.AlterTableStmt;
import com.premiumminds.sonar.plpgsql.rules.CreateStmt;
import com.premiumminds.sonar.plpgsql.rules.DropStmt;
import com.premiumminds.sonar.plpgsql.rules.DropdbStmt;
import com.premiumminds.sonar.plpgsql.rules.IndexStmt;
import com.premiumminds.sonar.plpgsql.rules.RenameStmt;
import com.premiumminds.sonar.plpgsql.rules.Stmt;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class PlPgSqlSensor implements Sensor {

    private static final Logger LOGGER = Loggers.get(PlPgSqlSensor.class);

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name("Add issues on line 1 of all Java files");
        descriptor.onlyOnLanguage(PlPgSqlLanguage.KEY);
        descriptor.createIssuesForRuleRepositories(PlPgSqlRulesDefinition.REPOSITORY);
    }

    @Override
    public void execute(SensorContext context) {
        FileSystem fs = context.fileSystem();
        Iterable<InputFile> files = fs.inputFiles(fs.predicates().hasLanguage(PlPgSqlLanguage.KEY));
        for (InputFile file : files) {

            try {
                final String contents = file.contents();
                final List<Integer> eolOffsets = parseEolOffsets(contents);

                final PgQueryParseResult.ByValue result = PGQueryLibrary.INSTANCE.pg_query_parse(contents);
                if (result.error != null){
                    LOGGER.error(result.error.message.getString(0));

                    final TextPointer textPointer = convertAbsoluteOffsetToTextPointer(file, eolOffsets, result.error.cursorpos);
                    NewIssue newIssue = context.newIssue()
                            .forRule(PlPgSqlRulesDefinition.RULE_PARSE_ERROR);
                    NewIssueLocation primaryLocation = newIssue.newLocation()
                            .on(file)
                            .at(file.selectLine(textPointer.line()))
                            .message("Failure to parse statement");
                    newIssue.at(primaryLocation);
                    newIssue.save();

                    continue;
                }

                parseTree(context, file, contents, eolOffsets, result.parse_tree.getString(0));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void parseTree(SensorContext context, InputFile file, String contents, List<Integer> eolOffsets, String result) {
        try (StringReader reader = new StringReader(result)) {
            try (JsonReader jsonReader = Json.createReader(reader)) {
                final JsonObject jsonObject = jsonReader.readObject();

                final JsonArray stmts = jsonObject.getJsonArray("stmts");
                stmts.forEach(jv -> {
                    final JsonObject jo = jv.asJsonObject();
                    final JsonObject stmt = jo.getJsonObject("stmt");

                    final TextRange textRange = parseTextRange(file, contents, eolOffsets, jo);

                    parseStatement(context, file, textRange, stmt);
                });
            }
        }
    }

    private TextRange parseTextRange(InputFile file, String contents, List<Integer> eolOffsets, JsonObject jo) {
        final int stmtLocation = jo.getInt("stmt_location", 0);
        final int stmtLen;
        if (jo.containsKey("stmt_len")) {
            stmtLen = jo.getInt("stmt_len");
        } else {
            stmtLen = contents.length() - stmtLocation;
        }
        final TextPointer textPointerStart = convertAbsoluteOffsetToTextPointer(file, eolOffsets, stmtLocation);
        final TextPointer textPointerEnd = convertAbsoluteOffsetToTextPointer(file, eolOffsets, stmtLocation + stmtLen);
        return file.newRange(textPointerStart, textPointerEnd);
    }

    private void parseStatement(SensorContext context, InputFile file, TextRange textRange, JsonObject jsonObject){
        jsonObject.forEach((key, value) -> {
            Stmt stmt;
            switch (key) {
                case "CreateStmt":
                    stmt = new CreateStmt();
                    break;
                case "IndexStmt":
                    stmt = new IndexStmt();
                    break;
                case "DropStmt":
                    stmt = new DropStmt();
                    break;
                case "DropdbStmt":
                    stmt = new DropdbStmt();
                    break;
                case "AlterTableStmt":
                    stmt = new AlterTableStmt();
                    break;
                case "RenameStmt":
                    stmt = new RenameStmt();
                    break;
                default:
                    LOGGER.warn(key + " not defined");
                    return;
            }
            stmt.validate(context, file, textRange, value.asJsonObject());
        });
    }
    private List<Integer> parseEolOffsets(String contents){
        List<Integer> eolOffsets = new ArrayList<>();
        for (int k = 0 ; k< contents.length(); k++){
            final char c = contents.charAt(k);
            if (c == '\n'){
                eolOffsets.add(k);
            }
        }
        return eolOffsets;
    }
    private TextPointer convertAbsoluteOffsetToTextPointer(InputFile file, List<Integer> eolOffsets, int absoluteOffset){
        int previousEolOffset = 0;
        for (int k = 0; k < eolOffsets.size(); k++) {
            int line = k +1;
            final Integer nextEolOffset = eolOffsets.get(k);
            if (previousEolOffset <= absoluteOffset && absoluteOffset < nextEolOffset){
                return file.newPointer(line, absoluteOffset - previousEolOffset);
            }
            previousEolOffset = nextEolOffset;
        }
        // last line can end without an end of line character
        return file.newPointer(eolOffsets.size()+1, absoluteOffset - previousEolOffset);
    }
}
