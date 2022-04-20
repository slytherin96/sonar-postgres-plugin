package com.premiumminds.sonar.plpgsql;

import org.sonar.api.Plugin;

public class PlPgSqlExtension implements Plugin {
    @Override
    public void define(Context context) {

        context.addExtensions(PlPgSqlLanguage.class, PlPgSqlRulesDefinition.class, PlPgSqlQualityProfile.class);
        context.addExtensions(PlPgSqlLanguage.getProperties());

        context.addExtension( PlPgSqlSensor.class);
    }
}
