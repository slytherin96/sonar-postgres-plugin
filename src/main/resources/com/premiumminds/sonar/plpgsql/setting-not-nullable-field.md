Use a check constraint instead of setting a column as ``NOT NULL``.

== problem

Setting a column as ``NOT NULL`` requires an ``ACCESS EXCLUSIVE`` lock. Reads and writes will be disabled while this statement is running.

== solutions

=== setting an existing column as non-nullable

Instead of:

``sql
ALTER TABLE "core_recipe" ALTER COLUMN "foo" SET NOT NULL;
``

Use:

``sql
ALTER TABLE "core_recipe" ADD CONSTRAINT foo_not_null
    CHECK ("foo" IS NOT NULL) NOT VALID;
-- backfill column so it's not null
ALTER TABLE "core_recipe" VALIDATE CONSTRAINT foo_not_null;
``

Add a check constraint as ``NOT VALID`` to verify new rows and updates are, backfill the column so it no longer contains null values, validate the constraint to verify existing rows are valid.

See ["How not valid constraints work"](https://squawkhq.com/docs/constraint-missing-not-valid=how-not-valid-validate-works) for more information on adding constraints as ``NOT VALID``.
