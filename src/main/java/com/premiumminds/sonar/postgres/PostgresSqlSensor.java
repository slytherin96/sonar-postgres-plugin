package com.premiumminds.sonar.postgres;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.premiumminds.sonar.postgres.analyzers.AlterSeqStmtAnalyzer;
import com.premiumminds.sonar.postgres.analyzers.AlterTableStmtAnalyzer;
import com.premiumminds.sonar.postgres.analyzers.StmtAnalyzer;
import com.premiumminds.sonar.postgres.analyzers.CreateSeqStmtAnalyzer;
import com.premiumminds.sonar.postgres.analyzers.CreateStmtAnalyzer;
import com.premiumminds.sonar.postgres.analyzers.DropStmtAnalyzer;
import com.premiumminds.sonar.postgres.analyzers.DropdbStmtAnalyzer;
import com.premiumminds.sonar.postgres.analyzers.IndexStmtAnalyzer;
import com.premiumminds.sonar.postgres.analyzers.RenameStmtAnalyzer;
import com.premiumminds.sonar.postgres.libpg_query.PGQueryLibrary;
import com.premiumminds.sonar.postgres.libpg_query.PgQueryProtobufParseResult;
import com.premiumminds.sonar.postgres.libpg_query.PgQueryScanResult;
import com.premiumminds.sonar.postgres.protobuf.ParseResult;
import com.premiumminds.sonar.postgres.protobuf.RawStmt;
import com.premiumminds.sonar.postgres.protobuf.ScanResult;
import com.premiumminds.sonar.postgres.protobuf.ScanToken;
import com.premiumminds.sonar.postgres.protobuf.Token;
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

public class PostgresSqlSensor implements Sensor {

    private static final Logger LOGGER = Loggers.get(PostgresSqlSensor.class);

    private static final int POSTGRESQL_MAX_IDENTIFIER_LENGTH = 63;

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name("Analyzes issues on Postgresql script files");
        descriptor.onlyOnLanguage(PostgresSqlLanguage.KEY);
        descriptor.createIssuesForRuleRepositories(PostgresSqlRulesDefinition.REPOSITORY);
    }

    @Override
    public void execute(SensorContext context) {
        FileSystem fs = context.fileSystem();
        Iterable<InputFile> files = fs.inputFiles(fs.predicates().hasLanguage(PostgresSqlLanguage.KEY));
        for (InputFile file : files) {

            try {
                final String unixContents = file.contents().replace("\r", "");
                final List<Integer> eolOffsets = parseEolOffsets(unixContents);

                parseContents(context, file, unixContents, eolOffsets);

                scanContents(context, file, unixContents, eolOffsets);
            } catch (Exception e) {
                LOGGER.error("problem parsing file: " + file.filename(), e);
                throw new RuntimeException(e);
            }
        }
    }

    private void scanContents(SensorContext context, InputFile file, String contents, List<Integer> eolOffsets) throws InvalidProtocolBufferException {
        final PgQueryScanResult.ByValue result = PGQueryLibrary.INSTANCE.pg_query_scan(contents);
        if (result.error != null){
            LOGGER.error("problem with file " + file.filename() + " at " + result.error.cursorpos + ": " + result.error.message.getString(0));

            final TextPointer textPointer = convertAbsoluteOffsetToTextPointer(file, eolOffsets, result.error.cursorpos);
            NewIssue newIssue = context.newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PARSE_ERROR);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(file.selectLine(textPointer.line()))
                    .message("Failure to scan statement");
            newIssue.at(primaryLocation);
            newIssue.save();
        }
        final ScanResult scanResult = ScanResult.parseFrom(result.pbuf.data.getByteArray(0, result.pbuf.len));
        scanResult.getTokensList()
                .stream()
                .filter(st -> st.getToken().equals(Token.IDENT))
                .filter(st -> contents.substring(st.getStart(), st.getEnd()).length() > POSTGRESQL_MAX_IDENTIFIER_LENGTH)
                .forEach(st -> {
                    final String identifier = contents.substring(st.getStart(), st.getEnd());
                    NewIssue newIssue = context.newIssue()
                            .forRule(PostgresSqlRulesDefinition.RULE_IDENTIFIER_MAX_LENGTH);
                    NewIssueLocation primaryLocation = newIssue.newLocation()
                            .on(file)
                            .at(parseTextRange(file, eolOffsets, st))
                            .message("Identifier '" + identifier + "' length (" + identifier.length() + ") is bigger than default maximum for Postgresql " + POSTGRESQL_MAX_IDENTIFIER_LENGTH);
                    newIssue.at(primaryLocation);
                    newIssue.save();
                });

        PGQueryLibrary.INSTANCE.pg_query_free_scan_result(result);
    }

    private void parseContents(SensorContext context, InputFile file, String contents, List<Integer> eolOffsets) throws InvalidProtocolBufferException {
        final PgQueryProtobufParseResult.ByValue result = PGQueryLibrary.INSTANCE.pg_query_parse_protobuf(contents);
        if (result.error != null){
            LOGGER.error("problem with file " + file.filename() + " at " + result.error.cursorpos + ": " + result.error.message.getString(0));

            final TextPointer textPointer = convertAbsoluteOffsetToTextPointer(file, eolOffsets, result.error.cursorpos);
            NewIssue newIssue = context.newIssue()
                    .forRule(PostgresSqlRulesDefinition.RULE_PARSE_ERROR);
            NewIssueLocation primaryLocation = newIssue.newLocation()
                    .on(file)
                    .at(file.selectLine(textPointer.line()))
                    .message("Failure to parse statement");
            newIssue.at(primaryLocation);
            newIssue.save();

            PGQueryLibrary.INSTANCE.pg_query_free_protobuf_parse_result(result);

            return;
        }

        final ParseResult parseResult = ParseResult.parseFrom(result.parse_tree.data.getByteArray(0, result.parse_tree.len));

        parseTree(context, file, contents, eolOffsets, parseResult);

        PGQueryLibrary.INSTANCE.pg_query_free_protobuf_parse_result(result);
    }

    private void parseTree(SensorContext context, InputFile file, String contents, List<Integer> eolOffsets, ParseResult result) {
        result.getStmtsList().forEach(stmt -> {
            final TextRange textRange = parseTextRange(file, contents, eolOffsets, stmt);
            parseStatement(context, file, textRange, stmt);
        });
    }

    private TextRange parseTextRange(InputFile file, List<Integer> eolOffsets, ScanToken st) {
        final TextPointer textPointerStart = convertAbsoluteOffsetToTextPointer(file, eolOffsets, st.getStart());
        final TextPointer textPointerEnd = convertAbsoluteOffsetToTextPointer(file, eolOffsets, st.getEnd());
        return file.newRange(textPointerStart, textPointerEnd);
    }

    private TextRange parseTextRange(InputFile file, String contents, List<Integer> eolOffsets, RawStmt stmt) {
        final int stmtLocation = stmt.getStmtLocation();
        final int stmtLen;
        if (stmt.getStmtLen() != 0) {
            stmtLen = stmt.getStmtLen();
        } else {
            stmtLen = contents.length() - stmtLocation - 1;
        }
        final TextPointer textPointerStart = convertAbsoluteOffsetToTextPointer(file, eolOffsets, stmtLocation);
        final TextPointer textPointerEnd = convertAbsoluteOffsetToTextPointer(file, eolOffsets, stmtLocation + stmtLen);
        return file.newRange(textPointerStart, textPointerEnd);
    }

    private void parseStatement(SensorContext context, InputFile file, TextRange textRange, RawStmt rawStmt){
        StmtAnalyzer stmtAnalyzer = null;
        if (rawStmt.getStmt().hasCreateStmt()) {
            stmtAnalyzer = new CreateStmtAnalyzer(rawStmt.getStmt().getCreateStmt());
        } else if (rawStmt.getStmt().hasIndexStmt()){
            stmtAnalyzer = new IndexStmtAnalyzer(rawStmt.getStmt().getIndexStmt());
        } else if (rawStmt.getStmt().hasDropStmt()){
            stmtAnalyzer = new DropStmtAnalyzer(rawStmt.getStmt().getDropStmt());
        } else if (rawStmt.getStmt().hasDropdbStmt()){
            stmtAnalyzer = new DropdbStmtAnalyzer(rawStmt.getStmt().getDropdbStmt());
        } else if (rawStmt.getStmt().hasAlterTableStmt()){
            stmtAnalyzer = new AlterTableStmtAnalyzer(rawStmt.getStmt().getAlterTableStmt());
        } else if (rawStmt.getStmt().hasRenameStmt()){
            stmtAnalyzer = new RenameStmtAnalyzer(rawStmt.getStmt().getRenameStmt());
        } else if (rawStmt.getStmt().hasCreateSeqStmt()){
            stmtAnalyzer = new CreateSeqStmtAnalyzer(rawStmt.getStmt().getCreateSeqStmt());
        } else if (rawStmt.getStmt().hasAlterSeqStmt()){
            stmtAnalyzer = new AlterSeqStmtAnalyzer(rawStmt.getStmt().getAlterSeqStmt());
        }

        if (stmtAnalyzer != null) {
            stmtAnalyzer.validate(context, file, textRange);
        }
    }

    private List<Integer> parseEolOffsets(String contents){
        List<Integer> eolOffsets = new ArrayList<>();
        for (int k = 0 ; k < contents.length(); k++){
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
