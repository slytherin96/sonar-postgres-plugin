package com.premiumminds.sonar.plpgsql;

import org.sonar.api.batch.rule.Rules;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

public class PlPgSqlRulesDefinition implements RulesDefinition {

    public static final String REPOSITORY = "plpgsql-repository";

    public static final RuleKey RULE_PARSE_ERROR = RuleKey.of(REPOSITORY, "parse-error");
    public static final RuleKey RULE_IF_NOT_EXISTS = RuleKey.of(REPOSITORY, "if-not-exists");
    public static final RuleKey RULE_IF_EXISTS = RuleKey.of(REPOSITORY, "if-exists");
    public static final RuleKey RULE_CONCURRENTLY = RuleKey.of(REPOSITORY, "concurrently");
    public static final RuleKey RULE_ADD_FIELD_WITH_DEFAULT = RuleKey.of(REPOSITORY, "adding-field-with-default");
    public static final RuleKey RULE_ADD_FOREIGN_KEY = RuleKey.of(REPOSITORY, "adding-foreign-key-constraint");
    public static final RuleKey RULE_SETTING_NOT_NULLABLE_FIELD = RuleKey.of(REPOSITORY, "setting-not-nullable-field");
    public static final RuleKey RULE_ADDING_SERIAL_PRIMARY_KEY_FIELD = RuleKey.of(REPOSITORY, "adding-serial-primary-key-field");
    public static final RuleKey RULE_BAN_CHAR_FIELD = RuleKey.of(REPOSITORY, "ban-char-field");
    public static final RuleKey RULE_BAN_DROP_DATABASE = RuleKey.of(REPOSITORY, "ban-drop-database");
    public static final RuleKey RULE_CHANGING_COLUMN_TYPE = RuleKey.of(REPOSITORY, "changing-column-type");
    public static final RuleKey RULE_CONSTRAINT_MISSING_NOT_VALID = RuleKey.of(REPOSITORY, "constraint-missing-not-valid");
    public static final RuleKey RULE_DISALLOWED_UNIQUE_CONSTRAINT = RuleKey.of(REPOSITORY, "disallowed-unique-constraint");
    public static final RuleKey RULE_PREFER_TEXT_FIELD = RuleKey.of(REPOSITORY, "prefer-text-field");
    public static final RuleKey RULE_RENAMING_COLUMN = RuleKey.of(REPOSITORY, "renaming-column");
    public static final RuleKey RULE_RENAMING_TABLE = RuleKey.of(REPOSITORY, "renaming-table");

    @Override
    public void define(Context context) {

        NewRepository repository = context.createRepository(REPOSITORY, PlPgSqlLanguage.KEY);

        repository.createRule(RULE_PARSE_ERROR.rule())
                .setName("parse error rule")
                .setType(RuleType.BUG)
                .setSeverity(Severity.BLOCKER)
                .setHtmlDescription("Generates an issue when PL/pgSQL is no valid and fails to parse");

        repository.createRule(RULE_IF_NOT_EXISTS.rule())
                .setName("if-not-exists rule")
                .setHtmlDescription("Generates an issue when adding a table or a column does not have IF NOT EXISTS");

        repository.createRule(RULE_IF_EXISTS.rule())
                .setName("if-exists rule")
                .setHtmlDescription("Generates an issue when dropping a table or a column does not have IF EXISTS");

        repository.createRule(RULE_CONCURRENTLY.rule())
                .setName("concurrently rule")
                .setHtmlDescription("Generates an issue when index creation is not concurrently ");

        repository.createRule(RULE_ADD_FIELD_WITH_DEFAULT.rule())
                .setName("TODO")
                .setHtmlDescription("TODO");

        repository.createRule(RULE_ADD_FOREIGN_KEY.rule())
                .setName("TODO")
                .setHtmlDescription("TODO");

        repository.createRule(RULE_SETTING_NOT_NULLABLE_FIELD.rule())
                .setName("TODO")
                .setHtmlDescription("TODO");

        repository.createRule(RULE_ADDING_SERIAL_PRIMARY_KEY_FIELD.rule())
                .setName("TODO")
                .setHtmlDescription("TODO");

        repository.createRule(RULE_BAN_CHAR_FIELD.rule())
                .setName("TODO")
                .setHtmlDescription("TODO");

        repository.createRule(RULE_BAN_DROP_DATABASE.rule())
                .setName("TODO")
                .setHtmlDescription("TODO");

        repository.createRule(RULE_CHANGING_COLUMN_TYPE.rule())
                .setName("TODO")
                .setHtmlDescription("TODO");

        repository.createRule(RULE_CONSTRAINT_MISSING_NOT_VALID.rule())
                .setName("TODO")
                .setHtmlDescription("TODO");

        repository.createRule(RULE_DISALLOWED_UNIQUE_CONSTRAINT.rule())
                .setName("TODO")
                .setHtmlDescription("TODO");

        repository.createRule(RULE_PREFER_TEXT_FIELD.rule())
                .setName("TODO")
                .setHtmlDescription("TODO");

        repository.createRule(RULE_RENAMING_COLUMN.rule())
                .setName("TODO")
                .setHtmlDescription("TODO");

        repository.createRule(RULE_RENAMING_TABLE.rule())
                .setName("TODO")
                .setHtmlDescription("TODO");

        repository.done();
    }
}
