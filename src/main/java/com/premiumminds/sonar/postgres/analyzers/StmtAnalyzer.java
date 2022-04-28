package com.premiumminds.sonar.postgres.analyzers;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;

public interface StmtAnalyzer {

    void validate(SensorContext context, InputFile file, TextRange textRange);
}
