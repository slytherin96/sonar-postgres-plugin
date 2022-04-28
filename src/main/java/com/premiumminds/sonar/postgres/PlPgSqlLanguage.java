package com.premiumminds.sonar.postgres;

import java.util.Collections;
import java.util.List;

import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Qualifiers;

public class PlPgSqlLanguage extends AbstractLanguage {

    public static final String FILE_SUFFIXES_KEY = "sonar.postgres.file.suffixes";
    public static final String FILE_SUFFIXES_DEFAULT_VALUE = ".sql";

    public static final String NAME = "Postgres SQL";
    public static final String KEY = "postgres-language";

    private final Configuration config;

    public PlPgSqlLanguage(Configuration config) {
        super(KEY, NAME);
        this.config = config;
    }

    @Override
    public String[] getFileSuffixes() {
        return config.getStringArray(FILE_SUFFIXES_KEY);
    }

    public static List<PropertyDefinition> getProperties() {
        return Collections.singletonList(PropertyDefinition.builder(FILE_SUFFIXES_KEY)
                .multiValues(true)
                .defaultValue(FILE_SUFFIXES_DEFAULT_VALUE)
                .category("sql")
                .name("File Suffixes")
                .description("List of suffixes for files to analyze.")
                .onQualifiers(Qualifiers.PROJECT)
                .build());
    }
}
