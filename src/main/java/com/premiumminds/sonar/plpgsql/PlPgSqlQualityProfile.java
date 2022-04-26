package com.premiumminds.sonar.plpgsql;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_ADDING_SERIAL_PRIMARY_KEY_FIELD;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_ADD_FIELD_WITH_DEFAULT;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_ADD_FOREIGN_KEY;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_BAN_CHAR_FIELD;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_BAN_DROP_DATABASE;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_CHANGING_COLUMN_TYPE;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_CONCURRENTLY;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_CONSTRAINT_MISSING_NOT_VALID;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_DISALLOWED_UNIQUE_CONSTRAINT;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_IF_EXISTS;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_IF_NOT_EXISTS;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_PARSE_ERROR;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_PREFER_TEXT_FIELD;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_RENAMING_COLUMN;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_RENAMING_TABLE;
import static com.premiumminds.sonar.plpgsql.PlPgSqlRulesDefinition.RULE_SETTING_NOT_NULLABLE_FIELD;

public class PlPgSqlQualityProfile implements BuiltInQualityProfilesDefinition {
    @Override
    public void define(Context context) {
        NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile("PL/pgSQL Rules", PlPgSqlLanguage.KEY);
        profile.setDefault(true);
        
        activateRule(profile, RULE_PARSE_ERROR);
        activateRule(profile, RULE_IF_NOT_EXISTS);
        activateRule(profile, RULE_IF_EXISTS);
        activateRule(profile, RULE_CONCURRENTLY);
        activateRule(profile, RULE_ADD_FIELD_WITH_DEFAULT);
        activateRule(profile, RULE_ADD_FOREIGN_KEY);
        activateRule(profile, RULE_SETTING_NOT_NULLABLE_FIELD);
        activateRule(profile, RULE_ADDING_SERIAL_PRIMARY_KEY_FIELD);
        activateRule(profile, RULE_BAN_CHAR_FIELD);
        activateRule(profile, RULE_BAN_DROP_DATABASE);
        activateRule(profile, RULE_CHANGING_COLUMN_TYPE);
        activateRule(profile, RULE_CONSTRAINT_MISSING_NOT_VALID);
        activateRule(profile, RULE_DISALLOWED_UNIQUE_CONSTRAINT);
        activateRule(profile, RULE_PREFER_TEXT_FIELD);
        activateRule(profile, RULE_RENAMING_COLUMN);
        activateRule(profile, RULE_RENAMING_TABLE);

        profile.done();
    }
    private void activateRule(NewBuiltInQualityProfile profile, RuleKey ruleKey){
        profile.activateRule(ruleKey.repository(), ruleKey.rule());
    }
}
