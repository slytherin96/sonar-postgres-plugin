
# Generate Java sources

[jnaerator](https://github.com/nativelibs4java/JNAerator) was used to manually generate Java sources from C headers. 

```shell
git clone http://github.com/nativelibs4java/JNAerator.git
cd JNAerator
JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64 \
    /usr/bin/mvn -DskipTests clean package

/usr/lib/jvm/java-1.11.0-openjdk-amd64/bin/java \
    -jar jnaerator/target/jnaerator-0.13-SNAPSHOT-shaded.jar
```