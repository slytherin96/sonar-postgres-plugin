
# Generate Java sources

 - Add these options to `protobuf/pg_query.proto`:
```
option java_multiple_files = true;
option java_package = "com.premiumminds.sonar.plpgsql.protobuf";
```

 - Run `protoc`:
```shell
/home/froque/Downloads/protoc-3.20.1-linux-x86_64/bin/protoc \
    --java_out=/home/froque/workspace/pm/sonar-plpgsql-plugin/src/main/java \
    --proto_path=/home/froque/workspace/opensource/libpg_query/protobuf/ \
    /home/froque/workspace/opensource/libpg_query/protobuf/pg_query.proto
```