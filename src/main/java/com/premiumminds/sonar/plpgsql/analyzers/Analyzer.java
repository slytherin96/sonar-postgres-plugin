package com.premiumminds.sonar.plpgsql.analyzers;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;

public interface Analyzer {

    void validate(SensorContext context, InputFile file, TextRange textRange);
}
