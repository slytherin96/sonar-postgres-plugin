== problem

Multiple migrations in the same file could lead to deadlocks if there is a single transaction for all migrations.

== solution

Use multiple files for multiple migrations.