package com.premiumminds.sonar.postgres;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlPgSqlSensorTest {

    SensorContextTester contextTester;

    @BeforeEach
    void before(){
        final Path file = Paths.get("");
        contextTester = SensorContextTester.create(file);
    }

    @Test
    void parseError() {

        createFile(contextTester, "file1.sql", "not valid sql;");

        PlPgSqlSensor sensor = new PlPgSqlSensor();
        sensor.execute(contextTester);

        Map<String, List<Issue>> issueMap = groupbyFile(contextTester.allIssues());

        assertEquals(1, issueMap.size());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("parse-error", issueMap.get(":file1.sql").get(0).ruleKey().rule());
        assertEquals("Failure to parse statement",
                issueMap.get(":file1.sql").get(0).primaryLocation().message());
    }

    @Test
    void maxIdentifier() {

        createFile(contextTester, "file1.sql",
                "create table if not exists a23456789_123456789_123456789_123456789_123456789_123456789_123456789_ (id int);");
        createFile(contextTester, "file2.sql",
                "create table if not exists foo (a23456789_123456789_123456789_123456789_123456789_123456789_123456789_ int);");
        createFile(contextTester, "file3.sql",
                "create index concurrently if not exists a23456789_123456789_123456789_123456789_123456789_123456789_123456789_ on foo (id);");

        PlPgSqlSensor sensor = new PlPgSqlSensor();
        sensor.execute(contextTester);

        Map<String, List<Issue>> issueMap = groupbyFile(contextTester.allIssues());

        assertEquals(3, issueMap.size());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("identifier-max-length", issueMap.get(":file1.sql").get(0).ruleKey().rule());
        assertEquals("Identifier 'a23456789_123456789_123456789_123456789_123456789_123456789_123456789_' length (70) is bigger than default maximum for Postgresql 63",
                issueMap.get(":file1.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file2.sql").size());
        assertEquals("identifier-max-length", issueMap.get(":file2.sql").get(0).ruleKey().rule());
        assertEquals("Identifier 'a23456789_123456789_123456789_123456789_123456789_123456789_123456789_' length (70) is bigger than default maximum for Postgresql 63",
                issueMap.get(":file2.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file3.sql").size());
        assertEquals("identifier-max-length", issueMap.get(":file3.sql").get(0).ruleKey().rule());
        assertEquals("Identifier 'a23456789_123456789_123456789_123456789_123456789_123456789_123456789_' length (70) is bigger than default maximum for Postgresql 63",
                issueMap.get(":file3.sql").get(0).primaryLocation().message());
    }

    @Test
    void createSequenceStatement() {

        createFile(contextTester, "file1.sql", "CREATE SEQUENCE foo START 101;");

        PlPgSqlSensor sensor = new PlPgSqlSensor();
        sensor.execute(contextTester);

        Map<String, List<Issue>> issueMap = groupbyFile(contextTester.allIssues());

        assertEquals(1, issueMap.size());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("prefer-robust-stmts", issueMap.get(":file1.sql").get(0).ruleKey().rule());
        assertEquals("Add IF NOT EXISTS to CREATE SEQUENCE foo",
                issueMap.get(":file1.sql").get(0).primaryLocation().message());
    }

    @Test
    void alterSequenceStatement() {

        createFile(contextTester, "file1.sql", "ALTER SEQUENCE foo RESTART WITH 105;");

        PlPgSqlSensor sensor = new PlPgSqlSensor();
        sensor.execute(contextTester);

        Map<String, List<Issue>> issueMap = groupbyFile(contextTester.allIssues());

        assertEquals(1, issueMap.size());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("prefer-robust-stmts", issueMap.get(":file1.sql").get(0).ruleKey().rule());
        assertEquals("Add IF EXISTS to ALTER SEQUENCE foo",
                issueMap.get(":file1.sql").get(0).primaryLocation().message());
    }

    @Test
    void alterIndexStatement() {

        createFile(contextTester, "file1.sql", "ALTER INDEX foo RENAME TO bar;");

        PlPgSqlSensor sensor = new PlPgSqlSensor();
        sensor.execute(contextTester);

        Map<String, List<Issue>> issueMap = groupbyFile(contextTester.allIssues());

        assertEquals(1, issueMap.size());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("prefer-robust-stmts", issueMap.get(":file1.sql").get(0).ruleKey().rule());
        assertEquals("Add IF EXISTS to ALTER INDEX foo",
                issueMap.get(":file1.sql").get(0).primaryLocation().message());
    }

    @Test
    void dropStatement() {

        createFile(contextTester, "file1.sql", "drop table foo, bar;");
        createFile(contextTester, "file2.sql", "DROP DATABASE foo;");
        createFile(contextTester, "file3.sql", "DROP INDEX IF EXISTS idx1, idx2;");
        createFile(contextTester, "file4.sql", "DROP INDEX CONCURRENTLY idx1;");
        createFile(contextTester, "file5.sql", "DROP SEQUENCE foo, bar;");

        PlPgSqlSensor sensor = new PlPgSqlSensor();
        sensor.execute(contextTester);

        Map<String, List<Issue>> issueMap = groupbyFile(contextTester.allIssues());

        assertEquals(5, issueMap.size());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("prefer-robust-stmts", issueMap.get(":file1.sql").get(0).ruleKey().rule());
        assertEquals("Add IF EXISTS to DROP TABLE foo, bar",
                issueMap.get(":file1.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file2.sql").size());
        assertEquals("ban-drop-database", issueMap.get(":file2.sql").get(0).ruleKey().rule());
        assertEquals("Dropping a database may break existing clients.",
                issueMap.get(":file2.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file3.sql").size());
        assertEquals("concurrently", issueMap.get(":file3.sql").get(0).ruleKey().rule());
        assertEquals("Add CONCURRENTLY to DROP INDEX idx1, idx2",
                issueMap.get(":file3.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file4.sql").size());
        assertEquals("prefer-robust-stmts", issueMap.get(":file4.sql").get(0).ruleKey().rule());
        assertEquals("Add IF EXISTS to DROP INDEX idx1",
                issueMap.get(":file4.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file5.sql").size());
        assertEquals("prefer-robust-stmts", issueMap.get(":file5.sql").get(0).ruleKey().rule());
        assertEquals("Add IF EXISTS to DROP SEQUENCE foo, bar",
                issueMap.get(":file5.sql").get(0).primaryLocation().message());
    }

    @Test
    void createTableStatement() {

        createFile(contextTester, "file1.sql", "create table foo (id int);");
        createFile(contextTester, "file2.sql", "create table if not exists foo (id int, CONSTRAINT id_fk FOREIGN KEY (id) REFERENCES bar(id) );");
        createFile(contextTester, "file3.sql", "create table if not exists foo (id int REFERENCES bar(id) );");
        createFile(contextTester, "file4.sql", "create table if not exists foo (id int, name char(100) NOT NULL);");
        createFile(contextTester, "file5.sql", "create table if not exists foo (id int, name varchar(100) NOT NULL);");
        createFile(contextTester, "file6.sql", "create table if not exists foo (id int, name varchar NOT NULL);");
        createFile(contextTester, "file7.sql", "create table if not exists foo (id int\n)");

        PlPgSqlSensor sensor = new PlPgSqlSensor();
        sensor.execute(contextTester);

        Map<String, List<Issue>> issueMap = groupbyFile(contextTester.allIssues());

        assertEquals(5, issueMap.size());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("prefer-robust-stmts", issueMap.get(":file1.sql").get(0).ruleKey().rule());
        assertEquals("Add IF NOT EXISTS to CREATE TABLE foo",
                issueMap.get(":file1.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file2.sql").size());
        assertEquals("adding-foreign-key-constraint", issueMap.get(":file2.sql").get(0).ruleKey().rule());
        assertEquals("Adding a foreign key constraint requires a table scan and a SHARE ROW EXCLUSIVE lock on both tables, which blocks writes to each table.",
                issueMap.get(":file2.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file3.sql").size());
        assertEquals("adding-foreign-key-constraint", issueMap.get(":file3.sql").get(0).ruleKey().rule());
        assertEquals("Adding a foreign key constraint requires a table scan and a SHARE ROW EXCLUSIVE lock on both tables, which blocks writes to each table.",
                issueMap.get(":file3.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file4.sql").size());
        assertEquals("ban-char-field", issueMap.get(":file4.sql").get(0).ruleKey().rule());
        assertEquals("Using character is likely a mistake and should almost always be replaced by text or varchar.",
                issueMap.get(":file4.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file5.sql").size());
        assertEquals("prefer-text-field", issueMap.get(":file5.sql").get(0).ruleKey().rule());
        assertEquals("Changing the size of a varchar field requires an ACCESS EXCLUSIVE lock, that will prevent all reads and writes to the table.",
                issueMap.get(":file5.sql").get(0).primaryLocation().message());
    }

    @Test
    void createIndexStatement() {

        createFile(contextTester, "file1.sql", "create index if not exists idx1 on foo (id);");
        createFile(contextTester, "file2.sql", "create index concurrently idx1 on foo (id);");

        PlPgSqlSensor sensor = new PlPgSqlSensor();
        sensor.execute(contextTester);

        Map<String, List<Issue>> issueMap = groupbyFile(contextTester.allIssues());

        assertEquals(2, issueMap.size());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("concurrently", issueMap.get(":file1.sql").get(0).ruleKey().rule());
        assertEquals("Add CONCURRENTLY to CREATE INDEX idx1",
                issueMap.get(":file1.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file2.sql").size());
        assertEquals("prefer-robust-stmts", issueMap.get(":file2.sql").get(0).ruleKey().rule());
        assertEquals("Add IF NOT EXISTS to CREATE INDEX idx1",
                issueMap.get(":file2.sql").get(0).primaryLocation().message());

    }

    @Test
    void alterTableStatement() {

        createFile(contextTester, "file1.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN bar integer;");
        createFile(contextTester, "file2.sql", "ALTER TABLE IF EXISTS foo DROP COLUMN bar;");
        createFile(contextTester, "file3.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN IF NOT EXISTS bar integer DEFAULT random();");
        createFile(contextTester, "file4.sql", "ALTER TABLE IF EXISTS foo ADD CONSTRAINT fk_bar FOREIGN KEY (bar_id) REFERENCES bar (id);");
        createFile(contextTester, "file5.sql", "ALTER TABLE IF EXISTS foo ADD CONSTRAINT fk_bar FOREIGN KEY (bar_id) REFERENCES bar (id) NOT VALID;" +
                                                                "ALTER TABLE IF EXISTS foo VALIDATE CONSTRAINT fk_bar;");
        createFile(contextTester, "file6.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN IF NOT EXISTS bar_id int4 REFERENCES bar(id);");
        createFile(contextTester, "file7.sql", "ALTER TABLE IF EXISTS foo ALTER COLUMN id SET NOT NULL;");
        createFile(contextTester, "file8.sql", "ALTER TABLE IF EXISTS foo ADD PRIMARY KEY (id);");
        createFile(contextTester, "file8-ok.sql", "CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS foo_pk_idx ON foo (id); " +
                                                                "ALTER TABLE IF EXISTS foo ADD CONSTRAINT foo_pk PRIMARY KEY USING INDEX foo_pk_idx;");
        createFile(contextTester, "file9.sql", "ALTER TABLE foo ADD COLUMN IF NOT EXISTS id int;");
        createFile(contextTester, "file10.sql", "ALTER TABLE IF EXISTS foo DROP CONSTRAINT bar_constraint;");
        createFile(contextTester, "file11.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN IF NOT EXISTS name character;");
        createFile(contextTester, "file12.sql", "ALTER TABLE IF EXISTS foo ALTER COLUMN id TYPE bigint;");
        createFile(contextTester, "file13.sql", "ALTER TABLE IF EXISTS foo ADD CONSTRAINT positive_balance CHECK (balance >= 0);");
        createFile(contextTester, "file14.sql", "ALTER TABLE IF EXISTS foo ADD CONSTRAINT positive_balance CHECK (balance >= 0) NOT VALID;" +
                                                                "ALTER TABLE IF EXISTS foo VALIDATE CONSTRAINT positive_balance;");
        createFile(contextTester, "file15.sql", "ALTER TABLE IF EXISTS foo ADD CONSTRAINT field_name_constraint UNIQUE (field_name);");
        createFile(contextTester, "file15-ok.sql", "CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS foo_name_temp_idx ON foo (name);" +
                                                                "ALTER TABLE IF EXISTS foo " +
                                                                "   DROP CONSTRAINT IF EXISTS name_constraint," +
                                                                "   ADD CONSTRAINT name_constraint UNIQUE USING INDEX foo_name_temp_idx;");
        createFile(contextTester, "file16.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN IF NOT EXISTS name varchar(100);");
        createFile(contextTester, "file17.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN IF NOT EXISTS name varchar;");
        createFile(contextTester, "file18.sql", "ALTER TABLE IF EXISTS foo RENAME COLUMN bar TO baz;");
        createFile(contextTester, "file19.sql", "ALTER TABLE IF EXISTS foo RENAME TO bar;");
        createFile(contextTester, "file20.sql", "ALTER TABLE IF EXISTS foo ALTER COLUMN bar SET DEFAULT -1;");
        createFile(contextTester, "file21.sql", "ALTER TABLE IF EXISTS foo ALTER COLUMN bar SET DEFAULT random();");
        createFile(contextTester, "file22.sql", "ALTER INDEX foo SET (fillfactor = 75);");

        PlPgSqlSensor sensor = new PlPgSqlSensor();
        sensor.execute(contextTester);

        Map<String, List<Issue>> issueMap = groupbyFile(contextTester.allIssues());

        assertEquals(18, issueMap.size());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("prefer-robust-stmts", issueMap.get(":file1.sql").get(0).ruleKey().rule());
        assertEquals("Add IF NOT EXISTS to ADD COLUMN bar",
                issueMap.get(":file1.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file2.sql").size());
        assertEquals("prefer-robust-stmts", issueMap.get(":file2.sql").get(0).ruleKey().rule());
        assertEquals("Add IF EXISTS to DROP COLUMN bar",
                issueMap.get(":file2.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file3.sql").size());
        assertEquals("adding-field-with-default", issueMap.get(":file3.sql").get(0).ruleKey().rule());
        assertEquals("Adding a field with a VOLATILE default can cause table rewrites, which will take an ACCESS EXCLUSIVE lock on the table, blocking reads / writes while the statement is running.",
                issueMap.get(":file3.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file4.sql").size());
        assertEquals("adding-foreign-key-constraint", issueMap.get(":file4.sql").get(0).ruleKey().rule());
        assertEquals("Adding a foreign key constraint requires a table scan and a SHARE ROW EXCLUSIVE lock on both tables, which blocks writes to each table.",
                issueMap.get(":file4.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file6.sql").size());
        assertEquals("adding-foreign-key-constraint", issueMap.get(":file6.sql").get(0).ruleKey().rule());
        assertEquals("Adding a foreign key constraint requires a table scan and a SHARE ROW EXCLUSIVE lock on both tables, which blocks writes to each table.",
                issueMap.get(":file6.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file7.sql").size());
        assertEquals("setting-not-nullable-field", issueMap.get(":file7.sql").get(0).ruleKey().rule());
        assertEquals("Setting a column as NOT NULL will require a scan of the entire table. However, if a valid CHECK constraint is found which proves no NULL can exist, then the table scan is skipped.",
                issueMap.get(":file7.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file7.sql").size());
        assertEquals("adding-serial-primary-key-field", issueMap.get(":file8.sql").get(0).ruleKey().rule());
        assertEquals("If PRIMARY KEY is specified, and the index's columns are not already marked NOT NULL, then this command will attempt to do ALTER COLUMN SET NOT NULL against each such column. That requires a full table scan to verify the column(s) contain no nulls. In all other cases, this is a fast operation.",
                issueMap.get(":file8.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file9.sql").size());
        assertEquals("prefer-robust-stmts", issueMap.get(":file9.sql").get(0).ruleKey().rule());
        assertEquals("Add IF EXISTS to ALTER TABLE foo",
                issueMap.get(":file9.sql").get(0).primaryLocation().message());

        assertEquals(2, issueMap.get(":file10.sql").size());
        assertEquals("prefer-robust-stmts", issueMap.get(":file10.sql").get(0).ruleKey().rule());
        assertEquals("Add IF EXISTS to DROP CONSTRAINT bar_constraint",
                issueMap.get(":file10.sql").get(0).primaryLocation().message());
        assertEquals("drop-constraint-drops-index", issueMap.get(":file10.sql").get(1).ruleKey().rule());
        assertEquals("Dropping a primary or unique constraint also drops any index underlying the constraint",
                issueMap.get(":file10.sql").get(1).primaryLocation().message());

        assertEquals(1, issueMap.get(":file11.sql").size());
        assertEquals("ban-char-field", issueMap.get(":file11.sql").get(0).ruleKey().rule());
        assertEquals("Using character is likely a mistake and should almost always be replaced by text or varchar.",
                issueMap.get(":file11.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file12.sql").size());
        assertEquals("changing-column-type", issueMap.get(":file12.sql").get(0).ruleKey().rule());
        assertEquals("Changing a column type requires an ACCESS EXCLUSIVE lock on the table which blocks reads and writes while the table is rewritten.",
                issueMap.get(":file12.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file13.sql").size());
        assertEquals("constraint-missing-not-valid", issueMap.get(":file13.sql").get(0).ruleKey().rule());
        assertEquals("By default new constraints require a table scan and block writes to the table while that scan occurs.",
                issueMap.get(":file13.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file15.sql").size());
        assertEquals("disallowed-unique-constraint", issueMap.get(":file15.sql").get(0).ruleKey().rule());
        assertEquals("Adding a UNIQUE constraint requires an ACCESS EXCLUSIVE lock which blocks reads and writes to the table while the index is built.",
                issueMap.get(":file15.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file16.sql").size());
        assertEquals("prefer-text-field", issueMap.get(":file16.sql").get(0).ruleKey().rule());
        assertEquals("Changing the size of a varchar field requires an ACCESS EXCLUSIVE lock, that will prevent all reads and writes to the table.",
                issueMap.get(":file16.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file18.sql").size());
        assertEquals("renaming-column", issueMap.get(":file18.sql").get(0).ruleKey().rule());
        assertEquals("Renaming a column may break existing clients.",
                issueMap.get(":file18.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file19.sql").size());
        assertEquals("renaming-table", issueMap.get(":file19.sql").get(0).ruleKey().rule());
        assertEquals("Renaming a table may break existing clients that depend on the old table name.",
                issueMap.get(":file19.sql").get(0).primaryLocation().message());

        assertEquals(1, issueMap.get(":file22.sql").size());
        assertEquals("prefer-robust-stmts", issueMap.get(":file22.sql").get(0).ruleKey().rule());
        assertEquals("Add IF EXISTS to ALTER INDEX foo",
                issueMap.get(":file22.sql").get(0).primaryLocation().message());

    }

    private Map<String, List<Issue>> groupbyFile(Collection<Issue> allIssues) {
        return allIssues.stream()
                .collect(Collectors.groupingBy(x -> x.primaryLocation().inputComponent().key()));
    }

    private void createFile(SensorContextTester contextTester, String relativePath, String content) {
        contextTester.fileSystem().add(TestInputFileBuilder.create("", relativePath)
                .setLanguage(PlPgSqlLanguage.KEY)
                .setContents(content)
                .build());
    }
}