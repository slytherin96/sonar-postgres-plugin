package com.premiumminds.sonar.plpgsql;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

public class PlPgSqlRulesDefinition implements RulesDefinition {

    public static final String REPOSITORY = "plpgsql-repository";

    public static final RuleKey RULE_PARSE_ERROR = RuleKey.of(REPOSITORY, "parse-error");
    public static final RuleKey RULE_PREFER_ROBUST_STMTS = RuleKey.of(REPOSITORY, "prefer-robust-stmts");
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
    public static final RuleKey RULE_IDENTIFIER_MAX_LENGTH = RuleKey.of(REPOSITORY, "identifier-max-length");
    public static final RuleKey RULE_DROP_INDEX_DROPS_INDEX = RuleKey.of(REPOSITORY, "drop-constraint-drops-index");

    @Override
    public void define(Context context) {

        NewRepository repository = context.createRepository(REPOSITORY, PlPgSqlLanguage.KEY);

        repository.createRule(RULE_PARSE_ERROR.rule())
                .setName("parse error rule")
                .setType(RuleType.BUG)
                .setSeverity(Severity.BLOCKER)
                .setMarkdownDescription(getClass().getResource("parse-error.md"));

        repository.createRule(RULE_PREFER_ROBUST_STMTS.rule())
                .setName("prefer-robust-stmts rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("prefer-robust-stmts.md"));

        repository.createRule(RULE_CONCURRENTLY.rule())
                .setName("concurrently rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("concurrently.md"));

        repository.createRule(RULE_ADD_FIELD_WITH_DEFAULT.rule())
                .setName("adding-field-with-default rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("adding-field-with-default.md"));

        repository.createRule(RULE_ADD_FOREIGN_KEY.rule())
                .setName("adding-foreign-key-constraint rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("adding-foreign-key-constraint.md"));

        repository.createRule(RULE_SETTING_NOT_NULLABLE_FIELD.rule())
                .setName("setting-not-nullable-field rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("setting-not-nullable-field.md"));

        repository.createRule(RULE_ADDING_SERIAL_PRIMARY_KEY_FIELD.rule())
                .setName("adding-serial-primary-key-field rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("adding-serial-primary-key-field.md"));

        repository.createRule(RULE_BAN_CHAR_FIELD.rule())
                .setName("ban-char-field rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("ban-char-field.md"));

        repository.createRule(RULE_BAN_DROP_DATABASE.rule())
                .setName("ban-drop-database rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("ban-drop-database.md"));

        repository.createRule(RULE_CHANGING_COLUMN_TYPE.rule())
                .setName("changing-column-type rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("changing-column-type.md"));

        repository.createRule(RULE_CONSTRAINT_MISSING_NOT_VALID.rule())
                .setName("constraint-missing-not-valid rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("constraint-missing-not-valid.md"));

        repository.createRule(RULE_DISALLOWED_UNIQUE_CONSTRAINT.rule())
                .setName("disallowed-unique-constraint rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("disallowed-unique-constraint.md"));

        repository.createRule(RULE_PREFER_TEXT_FIELD.rule())
                .setName("prefer-text-field rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("prefer-text-field.md"));

        repository.createRule(RULE_RENAMING_COLUMN.rule())
                .setName("renaming-column rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("renaming-column.md"));

        repository.createRule(RULE_RENAMING_TABLE.rule())
                .setName("renaming-table rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("renaming-table.md"));

        repository.createRule(RULE_IDENTIFIER_MAX_LENGTH.rule())
                .setName("identifier-max-length rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("identifier-max-length.md"));

        repository.createRule(RULE_DROP_INDEX_DROPS_INDEX.rule())
                .setName("drop-constraint-drops-index rule")
                .setType(RuleType.BUG)
                .setMarkdownDescription(getClass().getResource("drop-constraint-drops-index.md"));

        repository.done();
    }
}
