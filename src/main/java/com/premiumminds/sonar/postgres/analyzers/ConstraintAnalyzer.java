package com.premiumminds.sonar.postgres.analyzers;

import com.premiumminds.sonar.postgres.protobuf.Constraint;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;

public interface ConstraintAnalyzer {

    void validate(SensorContext context, InputFile file, TextRange textRange, Constraint constraint);

}
