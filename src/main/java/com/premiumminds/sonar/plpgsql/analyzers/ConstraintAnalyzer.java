package com.premiumminds.sonar.plpgsql.analyzers;

import com.premiumminds.sonar.plpgsql.protobuf.Constraint;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.SensorContext;

public interface ConstraintAnalyzer {

    void validate(SensorContext context, InputFile file, TextRange textRange, Constraint constraint);

}
