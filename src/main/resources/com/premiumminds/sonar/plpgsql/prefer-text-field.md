== problem

Changing the size of a ``varchar`` field requires an ``ACCESS EXCLUSIVE`` lock, that will prevent all reads and writes to the table.

[Don't Do This - Don't use varchar(n) by default](https://wiki.postgresql.org/wiki/Don't_Do_This#Don.27t_use_varchar.28n.29_by_default)

== solution

Use a text field with a ``CHECK CONSTRAINT`` makes it easier to change the  max length.

Instead of:

``sql
CREATE TABLE "app_user" (
    "id" serial NOT NULL PRIMARY KEY,
    "email" varchar(100) NOT NULL
);
``

Use:

``sql
CREATE TABLE "app_user" (
    "id" serial NOT NULL PRIMARY KEY,
    "email" TEXT NOT NULL,
    CONSTRAINT "text_size" CHECK (LENGTH("email") <= 100)
);
``
