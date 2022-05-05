package com.premiumminds.sonar.postgres;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFileFilter;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.premiumminds.sonar.postgres.PostgresSqlLanguage.FILE_EXCLUSIONS_KEY;

public class PostgresSQLExclusionsFileFilter implements InputFileFilter {

    private static final Logger LOGGER = Loggers.get(PostgresSQLExclusionsFileFilter.class);

    private final String[] excludedPatterns;

    public PostgresSQLExclusionsFileFilter(Configuration configuration) {
        excludedPatterns = configuration.getStringArray(FILE_EXCLUSIONS_KEY);
    }

    @Override
    public boolean accept(InputFile inputFile) {

        if (!PostgresSqlLanguage.KEY.equals(inputFile.language())) {
            return true;
        }

        String relativePath = inputFile.uri().toString();
        if (WildcardPattern.match(WildcardPattern.create(excludedPatterns), relativePath)) {
            LOGGER.debug("File [" + inputFile.uri() + "] is excluded by '" + FILE_EXCLUSIONS_KEY + "' property and will not be analyzed");
            return false;
        }

        return true;
    }
}
