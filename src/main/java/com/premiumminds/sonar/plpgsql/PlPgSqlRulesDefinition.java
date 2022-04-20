package com.premiumminds.sonar.plpgsql;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;

public class PlPgSqlRulesDefinition implements RulesDefinition {

    public static final String REPOSITORY = "plpgsql-repository";

    public static final RuleKey RULE_IF_NOT_EXISTS = RuleKey.of(REPOSITORY, "if-not-exists");
    public static final RuleKey RULE_IF_EXISTS = RuleKey.of(REPOSITORY, "if-exists");
    public static final RuleKey RULE_CONCURRENTLY = RuleKey.of(REPOSITORY, "concurrently");

    @Override
    public void define(Context context) {

        NewRepository repository = context.createRepository(REPOSITORY, PlPgSqlLanguage.KEY);

        repository.createRule(RULE_IF_NOT_EXISTS.rule())
                .setName("if-not-exists rule")
                .setHtmlDescription("Generates an issue when adding a table or a column does not have IF NOT EXISTS");
        repository.createRule(RULE_IF_EXISTS.rule())
                .setName("if-exists rule")
                .setHtmlDescription("Generates an issue when dropping a table or a column does not have IF EXISTS");
        repository.createRule(RULE_CONCURRENTLY.rule())
                .setName("concurrently rule")
                .setHtmlDescription("Generates an issue when index creation is not concurrently ");

        repository.done();
    }
}
