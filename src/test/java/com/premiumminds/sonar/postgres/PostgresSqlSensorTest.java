package com.premiumminds.sonar.postgres;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.rule.RuleKey;

import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_ADDING_SERIAL_PRIMARY_KEY_FIELD;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_ADD_FIELD_WITH_DEFAULT;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_ADD_FOREIGN_KEY;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_BAN_CHAR_FIELD;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_BAN_DROP_DATABASE;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_CHANGING_COLUMN_TYPE;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_CONCURRENTLY;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_CONSTRAINT_MISSING_NOT_VALID;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_DISALLOWED_UNIQUE_CONSTRAINT;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_DROP_CONSTRAINT_DROPS_INDEX;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_IDENTIFIER_MAX_LENGTH;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_PARSE_ERROR;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_PREFER_ROBUST_STMTS;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_PREFER_TEXT_FIELD;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_RENAMING_COLUMN;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_RENAMING_TABLE;
import static com.premiumminds.sonar.postgres.PostgresSqlRulesDefinition.RULE_SETTING_NOT_NULLABLE_FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PostgresSqlSensorTest {

    SensorContextTester contextTester;

    @BeforeEach
    void before(){
        final Path file = Paths.get("");
        contextTester = SensorContextTester.create(file);
    }

    @Test
    void parseError() {

        createFile(contextTester, "file1.sql", "not valid sql;");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_PARSE_ERROR);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.size());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Failure to parse statement",
                issueMap.get(":file1.sql").get("parse-error").primaryLocation().message());
    }

    @Test
    void maxIdentifierLength() {

        createFile(contextTester, "file1.sql",
                "create table if not exists a23456789_123456789_123456789_123456789_123456789_123456789_123456789_ (id int);");
        createFile(contextTester, "file2.sql",
                "create table if not exists foo (a23456789_123456789_123456789_123456789_123456789_123456789_123456789_ int);");
        createFile(contextTester, "file3.sql",
                "create index concurrently if not exists a23456789_123456789_123456789_123456789_123456789_123456789_123456789_ on foo (id);");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_IDENTIFIER_MAX_LENGTH);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(3, issueMap.size());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Identifier 'a23456789_123456789_123456789_123456789_123456789_123456789_123456789_' length (70) is bigger than default maximum for Postgresql 63",
                issueMap.get(":file1.sql").get("identifier-max-length").primaryLocation().message());

        assertEquals(1, issueMap.get(":file2.sql").size());
        assertEquals("Identifier 'a23456789_123456789_123456789_123456789_123456789_123456789_123456789_' length (70) is bigger than default maximum for Postgresql 63",
                issueMap.get(":file2.sql").get("identifier-max-length").primaryLocation().message());

        assertEquals(1, issueMap.get(":file3.sql").size());
        assertEquals("Identifier 'a23456789_123456789_123456789_123456789_123456789_123456789_123456789_' length (70) is bigger than default maximum for Postgresql 63",
                issueMap.get(":file3.sql").get("identifier-max-length").primaryLocation().message());
    }

    @Test
    public void preferRobustStmts() {
        createFile(contextTester, "file1.sql", "CREATE SEQUENCE foo START 101;");
        createFile(contextTester, "file2.sql", "ALTER SEQUENCE foo RESTART WITH 105;");
        createFile(contextTester, "file3.sql", "ALTER INDEX foo RENAME TO bar;");
        createFile(contextTester, "file4.sql", "drop table foo, bar;");
        createFile(contextTester, "file5.sql", "DROP INDEX CONCURRENTLY idx1;");
        createFile(contextTester, "file6.sql", "DROP SEQUENCE foo, bar;");
        createFile(contextTester, "file7.sql", "create table foo (id int);");
        createFile(contextTester, "file8.sql", "create index concurrently idx1 on foo (id);");
        createFile(contextTester, "file9.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN bar integer;");
        createFile(contextTester, "file10.sql", "ALTER TABLE IF EXISTS foo DROP COLUMN bar;");
        createFile(contextTester, "file11.sql", "ALTER TABLE foo ADD COLUMN IF NOT EXISTS id int;");
        createFile(contextTester, "file12.sql", "ALTER TABLE IF EXISTS foo DROP CONSTRAINT bar_constraint;");
        createFile(contextTester, "file13.sql", "ALTER INDEX foo SET (fillfactor = 75);");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_PREFER_ROBUST_STMTS);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Add IF NOT EXISTS to CREATE SEQUENCE foo",
                issueMap.get(":file1.sql").get("prefer-robust-stmts").primaryLocation().message());

        assertEquals(1, issueMap.get(":file2.sql").size());
        assertEquals("Add IF EXISTS to ALTER SEQUENCE foo",
                issueMap.get(":file2.sql").get("prefer-robust-stmts").primaryLocation().message());

        assertEquals(1, issueMap.get(":file3.sql").size());
        assertEquals("Add IF EXISTS to ALTER INDEX foo",
                issueMap.get(":file3.sql").get("prefer-robust-stmts").primaryLocation().message());

        assertEquals(1, issueMap.get(":file4.sql").size());
        assertEquals("Add IF EXISTS to DROP TABLE foo, bar",
                issueMap.get(":file4.sql").get("prefer-robust-stmts").primaryLocation().message());

        assertEquals(1, issueMap.get(":file5.sql").size());
        assertEquals("Add IF EXISTS to DROP INDEX idx1",
                issueMap.get(":file5.sql").get("prefer-robust-stmts").primaryLocation().message());

        assertEquals(1, issueMap.get(":file6.sql").size());
        assertEquals("Add IF EXISTS to DROP SEQUENCE foo, bar",
                issueMap.get(":file6.sql").get("prefer-robust-stmts").primaryLocation().message());

        assertEquals(1, issueMap.get(":file7.sql").size());
        assertEquals("Add IF NOT EXISTS to CREATE TABLE foo",
                issueMap.get(":file7.sql").get("prefer-robust-stmts").primaryLocation().message());

        assertEquals(1, issueMap.get(":file8.sql").size());
        assertEquals("Add IF NOT EXISTS to CREATE INDEX idx1",
                issueMap.get(":file8.sql").get("prefer-robust-stmts").primaryLocation().message());

        assertEquals(1, issueMap.get(":file9.sql").size());
        assertEquals("Add IF NOT EXISTS to ADD COLUMN bar",
                issueMap.get(":file9.sql").get("prefer-robust-stmts").primaryLocation().message());

        assertEquals(1, issueMap.get(":file10.sql").size());
        assertEquals("Add IF EXISTS to DROP COLUMN bar",
                issueMap.get(":file10.sql").get("prefer-robust-stmts").primaryLocation().message());

        assertEquals(1, issueMap.get(":file11.sql").size());
        assertEquals("Add IF EXISTS to ALTER TABLE foo",
                issueMap.get(":file11.sql").get("prefer-robust-stmts").primaryLocation().message());

        assertEquals(1, issueMap.get(":file12.sql").size());
        assertEquals("Add IF EXISTS to DROP CONSTRAINT bar_constraint",
                issueMap.get(":file12.sql").get("prefer-robust-stmts").primaryLocation().message());

        assertEquals(1, issueMap.get(":file13.sql").size());
        assertEquals("Add IF EXISTS to ALTER INDEX foo",
                issueMap.get(":file13.sql").get("prefer-robust-stmts").primaryLocation().message());

        assertEquals(13, issueMap.size());
    }

    @Test
    public void banDropDatabase() {
        createFile(contextTester, "file1.sql", "DROP DATABASE foo;");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_BAN_DROP_DATABASE);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Dropping a database may break existing clients.",
                issueMap.get(":file1.sql").get("ban-drop-database").primaryLocation().message());

        assertEquals(1, issueMap.size());
    }

    @Test
    public void concurrently() {
        createFile(contextTester, "file1.sql", "DROP INDEX IF EXISTS idx1, idx2;");
        createFile(contextTester, "file2.sql", "create index if not exists idx1 on foo (id);");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_CONCURRENTLY);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Add CONCURRENTLY to DROP INDEX idx1, idx2",
                issueMap.get(":file1.sql").get("concurrently").primaryLocation().message());

        assertEquals(1, issueMap.get(":file2.sql").size());
        assertEquals("Add CONCURRENTLY to CREATE INDEX idx1",
                issueMap.get(":file2.sql").get("concurrently").primaryLocation().message());

        assertEquals(2, issueMap.size());
    }

    @Test
    public void addingForeignKeyConstraint() {

        createFile(contextTester, "file1.sql", "create table if not exists foo (id int, CONSTRAINT id_fk FOREIGN KEY (id) REFERENCES bar(id) );");
        createFile(contextTester, "file2.sql", "create table if not exists foo (id int REFERENCES bar(id) );");
        createFile(contextTester, "file3.sql", "ALTER TABLE IF EXISTS foo ADD CONSTRAINT fk_bar FOREIGN KEY (bar_id) REFERENCES bar (id);");
        createFile(contextTester, "file4.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN IF NOT EXISTS bar_id int4 REFERENCES bar(id);");
        createFile(contextTester, "file5-ok.sql", "ALTER TABLE IF EXISTS foo ADD CONSTRAINT fk_bar FOREIGN KEY (bar_id) REFERENCES bar (id) NOT VALID;" +
                                                                "ALTER TABLE IF EXISTS foo VALIDATE CONSTRAINT fk_bar;");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_ADD_FOREIGN_KEY);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Adding a foreign key constraint requires a table scan and a SHARE ROW EXCLUSIVE lock on both tables, which blocks writes to each table.",
                issueMap.get(":file1.sql").get("adding-foreign-key-constraint").primaryLocation().message());

        assertEquals(1, issueMap.get(":file2.sql").size());
        assertEquals("Adding a foreign key constraint requires a table scan and a SHARE ROW EXCLUSIVE lock on both tables, which blocks writes to each table.",
                issueMap.get(":file2.sql").get("adding-foreign-key-constraint").primaryLocation().message());

        assertEquals(1, issueMap.get(":file3.sql").size());
        assertEquals("Adding a foreign key constraint requires a table scan and a SHARE ROW EXCLUSIVE lock on both tables, which blocks writes to each table.",
                issueMap.get(":file3.sql").get("adding-foreign-key-constraint").primaryLocation().message());

        assertEquals(1, issueMap.get(":file4.sql").size());
        assertEquals("Adding a foreign key constraint requires a table scan and a SHARE ROW EXCLUSIVE lock on both tables, which blocks writes to each table.",
                issueMap.get(":file4.sql").get("adding-foreign-key-constraint").primaryLocation().message());

        assertEquals(4, issueMap.size());
    }

    @Test
    public void banCharField() {
        createFile(contextTester, "file1.sql", "create table if not exists foo (id int, name char(100) NOT NULL);");
        createFile(contextTester, "file2.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN IF NOT EXISTS name character;");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_BAN_CHAR_FIELD);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Using character is likely a mistake and should almost always be replaced by text or varchar.",
                issueMap.get(":file1.sql").get("ban-char-field").primaryLocation().message());

        assertEquals(1, issueMap.get(":file2.sql").size());
        assertEquals("Using character is likely a mistake and should almost always be replaced by text or varchar.",
                issueMap.get(":file2.sql").get("ban-char-field").primaryLocation().message());

        assertEquals(2, issueMap.size());
    }

    @Test
    public void preferTextField() {

        createFile(contextTester, "file1.sql", "create table if not exists foo (id int, name varchar(100) NOT NULL);");
        createFile(contextTester, "file2.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN IF NOT EXISTS name varchar(100);");
        createFile(contextTester, "file3-ok.sql", "create table if not exists foo (id int, name varchar NOT NULL);");
        createFile(contextTester, "file4-ok.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN IF NOT EXISTS name varchar;");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_PREFER_TEXT_FIELD);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Changing the size of a varchar field requires an ACCESS EXCLUSIVE lock, that will prevent all reads and writes to the table.",
                issueMap.get(":file1.sql").get("prefer-text-field").primaryLocation().message());

        assertEquals(1, issueMap.get(":file2.sql").size());
        assertEquals("Changing the size of a varchar field requires an ACCESS EXCLUSIVE lock, that will prevent all reads and writes to the table.",
                issueMap.get(":file2.sql").get("prefer-text-field").primaryLocation().message());

        assertEquals(2, issueMap.size());
    }

    @Test
    public void addingFieldWithDefault() {
        createFile(contextTester, "file1.sql", "ALTER TABLE IF EXISTS foo ADD COLUMN IF NOT EXISTS bar integer DEFAULT random();");
        createFile(contextTester, "file2-ok.sql", "ALTER TABLE IF EXISTS foo ALTER COLUMN bar SET DEFAULT -1;");
        createFile(contextTester, "file3-ok.sql", "ALTER TABLE IF EXISTS foo ALTER COLUMN bar SET DEFAULT random();");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_ADD_FIELD_WITH_DEFAULT);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Adding a field with a VOLATILE default can cause table rewrites, which will take an ACCESS EXCLUSIVE lock on the table, blocking reads / writes while the statement is running.",
                issueMap.get(":file1.sql").get("adding-field-with-default").primaryLocation().message());

        assertEquals(1, issueMap.size());
    }

    @Test
    public void settingNotNullableField() {

        createFile(contextTester, "file1.sql", "ALTER TABLE IF EXISTS foo ALTER COLUMN id SET NOT NULL;");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_SETTING_NOT_NULLABLE_FIELD);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Setting a column as NOT NULL will require a scan of the entire table. However, if a valid CHECK constraint is found which proves no NULL can exist, then the table scan is skipped.",
                issueMap.get(":file1.sql").get("setting-not-nullable-field").primaryLocation().message());

        assertEquals(1, issueMap.size());
    }

    @Test
    public void addingSerialPrimaryKeyField() {

        createFile(contextTester, "file1.sql", "ALTER TABLE IF EXISTS foo ADD PRIMARY KEY (id);");
        createFile(contextTester, "file2-ok.sql", "CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS foo_pk_idx ON foo (id); " +
                                                                "ALTER TABLE IF EXISTS foo ADD CONSTRAINT foo_pk PRIMARY KEY USING INDEX foo_pk_idx;");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_ADDING_SERIAL_PRIMARY_KEY_FIELD);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("If PRIMARY KEY is specified, and the index's columns are not already marked NOT NULL, then this command will attempt to do ALTER COLUMN SET NOT NULL against each such column. That requires a full table scan to verify the column(s) contain no nulls. In all other cases, this is a fast operation.",
                issueMap.get(":file1.sql").get("adding-serial-primary-key-field").primaryLocation().message());

        assertEquals(1, issueMap.size());
    }

    @Test
    public void dropConstraintDropsIndex() {

        createFile(contextTester, "file1.sql", "ALTER TABLE IF EXISTS foo DROP CONSTRAINT bar_constraint;");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_DROP_CONSTRAINT_DROPS_INDEX);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Dropping a primary or unique constraint also drops any index underlying the constraint",
                issueMap.get(":file1.sql").get("drop-constraint-drops-index").primaryLocation().message());

        assertEquals(1, issueMap.size());
    }

    @Test
    public void changingColumnType() {
        createFile(contextTester, "file1.sql", "ALTER TABLE IF EXISTS foo ALTER COLUMN id TYPE bigint;");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_CHANGING_COLUMN_TYPE);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Changing a column type requires an ACCESS EXCLUSIVE lock on the table which blocks reads and writes while the table is rewritten.",
                issueMap.get(":file1.sql").get("changing-column-type").primaryLocation().message());

        assertEquals(1, issueMap.size());
    }

    @Test
    public void constraintMissingNotValid() {
        createFile(contextTester, "file1.sql", "ALTER TABLE IF EXISTS foo ADD CONSTRAINT positive_balance CHECK (balance >= 0);");
        createFile(contextTester, "file2-ok.sql", "ALTER TABLE IF EXISTS foo ADD CONSTRAINT positive_balance CHECK (balance >= 0) NOT VALID;" +
                                                                "ALTER TABLE IF EXISTS foo VALIDATE CONSTRAINT positive_balance;");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_CONSTRAINT_MISSING_NOT_VALID);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("By default new constraints require a table scan and block writes to the table while that scan occurs.",
                issueMap.get(":file1.sql").get("constraint-missing-not-valid").primaryLocation().message());

        assertEquals(1, issueMap.size());
    }

    @Test
    public void disallowedUniqueConstraint() {
        createFile(contextTester, "file1.sql", "ALTER TABLE IF EXISTS foo ADD CONSTRAINT field_name_constraint UNIQUE (field_name);");
        createFile(contextTester, "file2-ok.sql", "CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS foo_name_temp_idx ON foo (name);" +
                                                                "ALTER TABLE IF EXISTS foo " +
                                                                "   DROP CONSTRAINT IF EXISTS name_constraint," +
                                                                "   ADD CONSTRAINT name_constraint UNIQUE USING INDEX foo_name_temp_idx;");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_DISALLOWED_UNIQUE_CONSTRAINT);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Adding a UNIQUE constraint requires an ACCESS EXCLUSIVE lock which blocks reads and writes to the table while the index is built.",
                issueMap.get(":file1.sql").get("disallowed-unique-constraint").primaryLocation().message());

        assertEquals(1, issueMap.size());
    }

    @Test
    public void renamingColumn() {

        createFile(contextTester, "file1.sql", "ALTER TABLE IF EXISTS foo RENAME COLUMN bar TO baz;");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_RENAMING_COLUMN);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Renaming a column may break existing clients.",
                issueMap.get(":file1.sql").get("renaming-column").primaryLocation().message());

        assertEquals(1, issueMap.size());
    }

    @Test
    public void renamingTable() {
        createFile(contextTester, "file1.sql", "ALTER TABLE IF EXISTS foo RENAME TO bar;");

        PostgresSqlSensor sensor = getPostgresSqlSensor(RULE_RENAMING_TABLE);
        sensor.execute(contextTester);

        final Map<String, Map<String, Issue>> issueMap = groupByFile(contextTester.allIssues());

        assertEquals(1, issueMap.get(":file1.sql").size());
        assertEquals("Renaming a table may break existing clients that depend on the old table name.",
                issueMap.get(":file1.sql").get("renaming-table").primaryLocation().message());

        assertEquals(1, issueMap.size());
    }

    private PostgresSqlSensor getPostgresSqlSensor(RuleKey ruleKey) {
        ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();

        activeRulesBuilder
                .addRule(new NewActiveRule.Builder()
                        .setRuleKey(ruleKey)
                        .build());

        final DefaultActiveRules activeRules = activeRulesBuilder.build();

        CheckFactory checkFactory = new CheckFactory(activeRules);

        return new PostgresSqlSensor(checkFactory);
    }

    private Map<String, Map<String, Issue>> groupByFile(Collection<Issue> allIssues) {
        return allIssues.stream()
                .collect(Collectors.groupingBy(x -> x.primaryLocation().inputComponent().key(),
                        Collectors.toMap(y -> y.ruleKey().rule(), z -> z)));
    }

    private void createFile(SensorContextTester contextTester, String relativePath, String content) {
        contextTester.fileSystem().add(TestInputFileBuilder.create("", relativePath)
                .setLanguage(PostgresSqlLanguage.KEY)
                .setContents(content)
                .build());
    }
}