package com.premiumminds.sonar.postgres;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.premiumminds.sonar.postgres.libpg_query.PGQueryLibrary;
import com.premiumminds.sonar.postgres.libpg_query.PgQueryProtobufParseResult;
import com.premiumminds.sonar.postgres.libpg_query.PgQueryScanResult;
import com.premiumminds.sonar.postgres.protobuf.ParseResult;
import com.premiumminds.sonar.postgres.protobuf.RawStmt;
import com.premiumminds.sonar.postgres.protobuf.ScanResult;
import com.premiumminds.sonar.postgres.protobuf.ScanToken;
import com.premiumminds.sonar.postgres.protobuf.Token;
import com.premiumminds.sonar.postgres.visitors.AbstractVisitorCheck;
import com.premiumminds.sonar.postgres.visitors.AddFieldWithDefaultVisitorCheck;
import com.premiumminds.sonar.postgres.visitors.AddForeignKeyVisitorCheck;
import com.premiumminds.sonar.postgres.visitors.AddingSerialPrimaryKeyfieldvisitorCheck;
import com.premiumminds.sonar.postgres.visitors.BanCharFieldVisitorCheck;
import com.premiumminds.sonar.postgres.visitors.BanDropDatabaseVisitorCheck;
import com.premiumminds.sonar.postgres.visitors.ChangingColumnTypeVisitorCheck;
import com.premiumminds.sonar.postgres.visitors.ConcurrentVisitorCheck;
import com.premiumminds.sonar.postgres.visitors.ConstraintMissingNotValidVisitorCheck;
import com.premiumminds.sonar.postgres.visitors.DisallowedUniqueConstraintVisitorCheck;
import com.premiumminds.sonar.postgres.visitors.DropConstraintDropsIndexVisitorCheck;
import com.premiumminds.sonar.postgres.visitors.PreferTextFieldVisitorCheck;
import com.premiumminds.sonar.postgres.visitors.RenameColumnVisitorCheck;
import com.premiumminds.sonar.postgres.visitors.RenameTableVisitorCheck;
import com.premiumminds.sonar.postgres.visitors.RobustStatementsVisitorCheck;
import com.premiumminds.sonar.postgres.visitors.SettingNotNullVisitorCheck;
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

            if (context.isCancelled()) {
                throw new RuntimeException("Analysis cancelled");
            }

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
        final List<AbstractVisitorCheck> visitorChecks = Arrays.asList(
                new BanDropDatabaseVisitorCheck(),
                new ConcurrentVisitorCheck(),
                new RenameColumnVisitorCheck(),
                new RenameTableVisitorCheck(),
                new RobustStatementsVisitorCheck(),
                new SettingNotNullVisitorCheck(),
                new DropConstraintDropsIndexVisitorCheck(),
                new ChangingColumnTypeVisitorCheck(),
                new DisallowedUniqueConstraintVisitorCheck(),
                new AddingSerialPrimaryKeyfieldvisitorCheck(),
                new ConstraintMissingNotValidVisitorCheck(),
                new AddForeignKeyVisitorCheck(),
                new AddFieldWithDefaultVisitorCheck(),
                new BanCharFieldVisitorCheck(),
                new PreferTextFieldVisitorCheck());

        visitorChecks.forEach(visitorCheck -> visitorCheck.analyze(context, file, textRange, rawStmt));
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
