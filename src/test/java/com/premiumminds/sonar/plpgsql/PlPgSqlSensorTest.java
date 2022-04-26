package com.premiumminds.sonar.plpgsql;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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

        final List<Issue> issues = new ArrayList<>(contextTester.allIssues());
        assertEquals(1, issues.size());
        assertEquals("parse-error", issues.get(0).ruleKey().rule());
        assertEquals("Failure to parse statement", issues.get(0).primaryLocation().message());
        assertEquals(":file1.sql", issues.get(0).primaryLocation().inputComponent().key());
    }

    @Test
    void dropStatement() {

        createFile(contextTester, "file1.sql", "drop table foo;");
        createFile(contextTester, "file2.sql", "DROP DATABASE foo;");
        createFile(contextTester, "file3.sql", "DROP INDEX IF EXISTS foo_idx;");

        PlPgSqlSensor sensor = new PlPgSqlSensor();
        sensor.execute(contextTester);

        Map<String, Issue> issueMap = getIssueFileMap(contextTester.allIssues());

        assertEquals(3, issueMap.size());

        assertEquals("if-exists", issueMap.get(":file1.sql").ruleKey().rule());
        assertEquals("ban-drop-database", issueMap.get(":file2.sql").ruleKey().rule());
        assertEquals("concurrently", issueMap.get(":file3.sql").ruleKey().rule());
    }

    @Test
    void createTableStatement() {

        createFile(contextTester, "file1.sql", "create table foo (id int);");
        createFile(contextTester, "file2.sql", "create table if not exists foo (id int, CONSTRAINT id_fk FOREIGN KEY (id) REFERENCES bar(id) );");
        createFile(contextTester, "file3.sql", "create table if not exists foo (id int REFERENCES bar(id) );");
        createFile(contextTester, "file4.sql", "create table if not exists foo (id int, name char(100) NOT NULL);");
        createFile(contextTester, "file5.sql", "create table if not exists foo (id int, name varchar(100) NOT NULL);");
        createFile(contextTester, "file6.sql", "create table if not exists foo (id int, name varchar NOT NULL);");

        PlPgSqlSensor sensor = new PlPgSqlSensor();
        sensor.execute(contextTester);

        Map<String, Issue> issueMap = getIssueFileMap(contextTester.allIssues());

        assertEquals(5, issueMap.size());

        assertEquals("if-not-exists", issueMap.get(":file1.sql").ruleKey().rule());
        assertEquals("Add IF NOT EXISTS to CREATE TABLE foo", issueMap.get(":file1.sql").primaryLocation().message());

        assertEquals("adding-foreign-key-constraint", issueMap.get(":file2.sql").ruleKey().rule());
        assertEquals("Adding a foreign key constraint requires a table scan and a SHARE ROW EXCLUSIVE lock on both tables, which blocks writes to each table.",
                issueMap.get(":file2.sql").primaryLocation().message());

        assertEquals("adding-foreign-key-constraint", issueMap.get(":file3.sql").ruleKey().rule());
        assertEquals("Adding a foreign key constraint requires a table scan and a SHARE ROW EXCLUSIVE lock on both tables, which blocks writes to each table.",
                issueMap.get(":file3.sql").primaryLocation().message());

        assertEquals("ban-char-field", issueMap.get(":file4.sql").ruleKey().rule());
        assertEquals("Using character is likely a mistake and should almost always be replaced by text or varchar.",
                issueMap.get(":file4.sql").primaryLocation().message());

        assertEquals("prefer-text-field", issueMap.get(":file5.sql").ruleKey().rule());
        assertEquals("Changing the size of a varchar field requires an ACCESS EXCLUSIVE lock, that will prevent all reads and writes to the table.",
                issueMap.get(":file5.sql").primaryLocation().message());
    }

    @Test
    void createIndexStatement() {

        createFile(contextTester, "file1.sql", "create index if not exists idx1 on foo (id);");
        createFile(contextTester, "file2.sql", "create index concurrently idx1 on foo (id);");

        PlPgSqlSensor sensor = new PlPgSqlSensor();
        sensor.execute(contextTester);

        Map<String, Issue> issueMap = getIssueFileMap(contextTester.allIssues());

        assertEquals(2, issueMap.size());

        assertEquals("concurrently", issueMap.get(":file1.sql").ruleKey().rule());
        assertEquals("Add CONCURRENTLY to CREATE INDEX idx1", issueMap.get(":file1.sql").primaryLocation().message());

        assertEquals("if-not-exists", issueMap.get(":file2.sql").ruleKey().rule());
        assertEquals("Add IF NOT EXISTS to CREATE INDEX idx1", issueMap.get(":file2.sql").primaryLocation().message());

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
        createFile(contextTester, "file9.sql", "ALTER TABLE foo ADD COLUMN IF NOT EXISTS id int;");
        createFile(contextTester, "file10.sql", "ALTER TABLE IF EXISTS foo DROP CONSTRAINT bar_constraint;");
        createFile(contextTester, "file11.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN IF NOT EXISTS name character;");
        createFile(contextTester, "file12.sql", "ALTER TABLE IF EXISTS foo ALTER COLUMN id TYPE bigint;");
        createFile(contextTester, "file13.sql", "ALTER TABLE IF EXISTS foo ADD CONSTRAINT positive_balance CHECK (balance >= 0);");
        createFile(contextTester, "file14.sql", "ALTER TABLE IF EXISTS foo ADD CONSTRAINT positive_balance CHECK (balance >= 0) NOT VALID;" +
                                                                "ALTER TABLE IF EXISTS foo VALIDATE CONSTRAINT positive_balance;");
        createFile(contextTester, "file15.sql", "ALTER TABLE IF EXISTS foo ADD CONSTRAINT field_name_constraint UNIQUE (field_name);");
        createFile(contextTester, "file16.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN IF NOT EXISTS name varchar(100);");
        createFile(contextTester, "file17.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN IF NOT EXISTS name varchar;");
        createFile(contextTester, "file18.sql", "ALTER TABLE IF EXISTS foo RENAME COLUMN bar TO baz;");
        createFile(contextTester, "file19.sql", "ALTER TABLE IF EXISTS foo RENAME TO bar;");

        PlPgSqlSensor sensor = new PlPgSqlSensor();
        sensor.execute(contextTester);

        Map<String, Issue> issueMap = getIssueFileMap(contextTester.allIssues());

        assertEquals(16, issueMap.size());

        assertEquals("if-not-exists", issueMap.get(":file1.sql").ruleKey().rule());
        assertEquals("Add IF NOT EXISTS to ADD COLUMN bar", issueMap.get(":file1.sql").primaryLocation().message());

        assertEquals("if-exists", issueMap.get(":file2.sql").ruleKey().rule());
        assertEquals("Add IF EXISTS to DROP COLUMN bar", issueMap.get(":file2.sql").primaryLocation().message());

        assertEquals("adding-field-with-default", issueMap.get(":file3.sql").ruleKey().rule());
        assertEquals("Adding a field with a VOLATILE default can cause table rewrites, which will take an ACCESS EXCLUSIVE lock on the table, blocking reads / writes while the statement is running.",
                issueMap.get(":file3.sql").primaryLocation().message());

        assertEquals("adding-foreign-key-constraint", issueMap.get(":file4.sql").ruleKey().rule());
        assertEquals("Adding a foreign key constraint requires a table scan and a SHARE ROW EXCLUSIVE lock on both tables, which blocks writes to each table.",
                issueMap.get(":file4.sql").primaryLocation().message());

        assertEquals("adding-foreign-key-constraint", issueMap.get(":file6.sql").ruleKey().rule());
        assertEquals("Adding a foreign key constraint requires a table scan and a SHARE ROW EXCLUSIVE lock on both tables, which blocks writes to each table.",
                issueMap.get(":file6.sql").primaryLocation().message());

        assertEquals("setting-not-nullable-field", issueMap.get(":file7.sql").ruleKey().rule());
        assertEquals("Ordinarily this is checked during the ALTER TABLE by scanning the entire table;",
                issueMap.get(":file7.sql").primaryLocation().message());

        assertEquals("adding-serial-primary-key-field", issueMap.get(":file8.sql").ruleKey().rule());
        assertEquals("If PRIMARY KEY is specified, and the index's columns are not already marked NOT NULL, then this command will attempt to do ALTER COLUMN SET NOT NULL against each such column. That requires a full table scan to verify the column(s) contain no nulls. In all other cases, this is a fast operation.",
                issueMap.get(":file8.sql").primaryLocation().message());

        assertEquals("if-exists", issueMap.get(":file9.sql").ruleKey().rule());
        assertEquals("Add IF EXISTS to ALTER TABLE foo", issueMap.get(":file9.sql").primaryLocation().message());

        assertEquals("if-exists", issueMap.get(":file10.sql").ruleKey().rule());
        assertEquals("Add IF EXISTS to DROP CONSTRAINT bar_constraint", issueMap.get(":file10.sql").primaryLocation().message());

        assertEquals("ban-char-field", issueMap.get(":file11.sql").ruleKey().rule());
        assertEquals("Using character is likely a mistake and should almost always be replaced by text or varchar.",
                issueMap.get(":file11.sql").primaryLocation().message());

        assertEquals("changing-column-type", issueMap.get(":file12.sql").ruleKey().rule());
        assertEquals("Changing a column type requires an ACCESS EXCLUSIVE lock on the table which blocks reads and writes while the table is rewritten.",
                issueMap.get(":file12.sql").primaryLocation().message());

        assertEquals("constraint-missing-not-valid", issueMap.get(":file13.sql").ruleKey().rule());
        assertEquals("By default new constraints require a table scan and block writes to the table while that scan occurs.",
                issueMap.get(":file13.sql").primaryLocation().message());

        assertEquals("disallowed-unique-constraint", issueMap.get(":file15.sql").ruleKey().rule());
        assertEquals("Adding a UNIQUE constraint requires an ACCESS EXCLUSIVE lock which blocks reads and writes to the table while the index is built.",
                issueMap.get(":file15.sql").primaryLocation().message());

        assertEquals("prefer-text-field", issueMap.get(":file16.sql").ruleKey().rule());
        assertEquals("Changing the size of a varchar field requires an ACCESS EXCLUSIVE lock, that will prevent all reads and writes to the table.",
                issueMap.get(":file16.sql").primaryLocation().message());

        assertEquals("renaming-column", issueMap.get(":file18.sql").ruleKey().rule());
        assertEquals("Renaming a column may break existing clients.",
                issueMap.get(":file18.sql").primaryLocation().message());

        assertEquals("renaming-table", issueMap.get(":file19.sql").ruleKey().rule());
        assertEquals("Renaming a table may break existing clients that depend on the old table name.",
                issueMap.get(":file19.sql").primaryLocation().message());

    }

    private Map<String, Issue> getIssueFileMap(Collection<Issue> allIssues) {
        return allIssues.stream()
                .collect(Collectors.toMap(x -> x.primaryLocation().inputComponent().key(), x -> x));
    }

    private void createFile(SensorContextTester contextTester, String relativePath, String content) {
        contextTester.fileSystem().add(TestInputFileBuilder.create("", relativePath)
                .setLanguage(PlPgSqlLanguage.KEY)
                .setContents(content)
                .build());
    }
}