package com.premiumminds.sonar.plpgsql.rules;

enum EnumConstraint {
    CONSTR_DEFAULT,
    CONSTR_UNIQUE,
    CONSTR_NOTNULL,
    CONSTR_IDENTITY,
    CONSTR_PRIMARY,
    CONSTR_FOREIGN,
    CONSTR_CHECK,
    CONSTR_NULL,
}
