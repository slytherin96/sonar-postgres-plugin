package com.premiumminds.sonar.plpgsql;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_CONCURRENTLY;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_IF_EXISTS;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_IF_NOT_EXISTS;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_PARSE_ERROR;

public class PlPgSqlQualityProfile implements BuiltInQualityProfilesDefinition {
    @Override
    public void define(Context context) {
        NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile("PL/pgSQL Rules", PlPgSqlLanguage.KEY);
        profile.setDefault(true);

        profile.activateRule(RULE_PARSE_ERROR.repository(), RULE_PARSE_ERROR.rule());
        profile.activateRule(RULE_CONCURRENTLY.repository(), RULE_IF_EXISTS.rule());
        profile.activateRule(RULE_CONCURRENTLY.repository(), RULE_IF_NOT_EXISTS.rule());
        profile.activateRule(RULE_CONCURRENTLY.repository(), RULE_CONCURRENTLY.rule());

        profile.done();
    }
}
