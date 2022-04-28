
# Generate Java sources

 - Add these options to `protobuf/pg_query.proto`:
```
option java_multiple_files = true;
option java_package = "com.premiumminds.sonar.postgres.protobuf";
```

 - Run `protoc`:
```shell
/home/froque/Downloads/protoc-3.20.1-linux-x86_64/bin/protoc \
    --java_out=/home/froque/workspace/pm/sonar-postgres-plugin/src/main/java \
    --proto_path=/home/froque/workspace/opensource/libpg_query/protobuf/ \
    /home/froque/workspace/opensource/libpg_query/protobuf/pg_query.proto
```

protoc creates a duplicate VALUES in Token.java. Manually rename `private static final Token[] VALUES = values();` to `private static final Token[] INTERNAL_VALUES = values();`
