package com.premiumminds.sonar.plpgsql.libpg_query;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;
import com.premiumminds.sonar.plpgsql.protobuf.ParseResult;
import com.premiumminds.sonar.plpgsql.protobuf.RawStmt;
import com.premiumminds.sonar.plpgsql.protobuf.ScanResult;
import com.premiumminds.sonar.plpgsql.protobuf.Token;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PGQueryLibraryTest {

    @Test
    void pg_query_parse() {
        final String query = "select bar from foo;";
        final PgQueryParseResult.ByValue result = PGQueryLibrary.INSTANCE.pg_query_parse(query);
        assertNull(result.error);

        assertEquals("{\"version\":130003,\"stmts\":[{\"stmt\":{\"SelectStmt\":{\"targetList\":[{\"ResTarget\":{\"val\":{\"ColumnRef\":{\"fields\":[{\"String\":{\"str\":\"bar\"}}],\"location\":7}},\"location\":7}}],\"fromClause\":[{\"RangeVar\":{\"relname\":\"foo\",\"inh\":true,\"relpersistence\":\"p\",\"location\":16}}],\"limitOption\":\"LIMIT_OPTION_DEFAULT\",\"op\":\"SETOP_NONE\"}},\"stmt_len\":19}]}",
                result.parse_tree.getString(0));

        PGQueryLibrary.INSTANCE.pg_query_free_parse_result(result);
    }

    @Test
    void pg_query_parse_protobuf() throws InvalidProtocolBufferException {
        final String query = "select bar from foo;";

        final PgQueryProtobufParseResult.ByValue result = PGQueryLibrary.INSTANCE.pg_query_parse_protobuf(query);
        assertNull(result.error);

        final ParseResult parseResult = ParseResult.parseFrom(result.parse_tree.data.getByteArray(0, result.parse_tree.len));
        assertEquals(1, parseResult.getStmtsCount());

        final RawStmt stmt = parseResult.getStmts(0);
        assertTrue(stmt.getStmt().hasSelectStmt());
        assertFalse(stmt.getStmt().hasIndexStmt());
        assertFalse(stmt.getStmt().hasInsertStmt());
        assertFalse(stmt.getStmt().hasDeleteStmt());
        assertFalse(stmt.getStmt().hasUpdateStmt());
        assertFalse(stmt.getStmt().hasCreateStmt());
        assertFalse(stmt.getStmt().hasDropStmt());

        assertEquals(1, stmt.getStmt().getSelectStmt().getTargetListCount());
        assertEquals(Collections.singletonList("bar"), stmt.getStmt()
                .getSelectStmt()
                .getTargetListList()
                .stream()
                .map(n -> n.getResTarget().getVal().getColumnRef().getFieldsList())
                .flatMap(List::stream)
                .map(x -> x.getString().getStr())
                .collect(Collectors.toList()));

        assertEquals(1, stmt.getStmt().getSelectStmt().getFromClauseCount());
        assertEquals(Collections.singletonList("foo"), stmt.getStmt()
                .getSelectStmt()
                .getFromClauseList()
                .stream()
                .map(n -> n.getRangeVar().getRelname())
                .collect(Collectors.toList()));

        PGQueryLibrary.INSTANCE.pg_query_free_protobuf_parse_result(result);
    }

    @Test
    void pg_query_scan() throws InvalidProtocolBufferException {
        final String query = "select bar from foo;";
        final PgQueryScanResult.ByValue result = PGQueryLibrary.INSTANCE.pg_query_scan(query);
        assertNull(result.error);

        final ScanResult scanResult = ScanResult.parseFrom(result.pbuf.data.getByteArray(0, result.pbuf.len));

        assertEquals(5, scanResult.getTokensCount());

        assertEquals(Arrays.asList("bar", "foo"), scanResult.getTokensList()
                .stream()
                .filter(st -> st.getToken().equals(Token.IDENT))
                .map(st -> query.substring(st.getStart(), st.getEnd()))
                .collect(Collectors.toList()));

        PGQueryLibrary.INSTANCE.pg_query_free_scan_result(result);

    }
}