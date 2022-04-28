package com.premiumminds.sonar.postgres;

import org.sonar.api.Plugin;

public class PotgresSQLExtension implements Plugin {
    @Override
    public void define(Context context) {

        context.addExtensions(PostgresSqlLanguage.class, PostgresSqlRulesDefinition.class, PostgresSqlQualityProfile.class);
        context.addExtensions(PostgresSqlLanguage.getProperties());

        context.addExtension( PostgresSqlSensor.class);
    }
}
