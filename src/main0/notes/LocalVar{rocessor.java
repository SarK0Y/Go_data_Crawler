/*
Below are concise examples showing how to compile and run the LocalVarProcessor with javac and with Maven.

1) Project layout
Assume this structure:
- project-root/
  - src/main/java/com/example/processor/LocalVarProcessor.java
  - src/main/java/com/example/anno/MyThing.java
  - src/main/java/com/example/app/Tzt.java

MyThing.java (simple marker annotation):
```java
package com.example.anno;
import java.lang.annotation.*;
@Target(ElementType.LOCAL_VARIABLE)
@Retention(RetentionPolicy.SOURCE)
public @interface MyThing {}
```

Tzt.java (sample code to be compiled and scanned):
```java
package com.example.app;
import com.example.anno.MyThing;
public class Tzt {
    public void tst() {
        @MyThing
        int x = 0;
    }
}
```

LocalVarProcessor.java
(use the full processor from the previous message; placed at com/example/processor/LocalVarProcessor.java).

----

2) Using javac (manual compile)
- Compile processor first and produce a JAR or classes directory:

javac -d out/processor \
  src/main/java/com/example/processor/LocalVarProcessor.java \
  src/main/java/com/example/anno/MyThing.java

- Compile the application sources with the processor on the processorpath:

javac -cp out/processor -processorpath out/processor \
  -processor com.example.processor.LocalVarProcessor \
  -d out/app \
  src/main/java/com/example/app/Tzt.java

- Check compiler output: the processor uses processingEnv.getMessager().printMessage(...) to print notes about found local-variable annotations. Those appear as compiler notes on stdout/stderr.

Notes:
- If using com.sun.source APIs you must run javac from a JDK (not a plain JRE). The -processor argument forces use of your processor.
- If you packaged the processor as a JAR (processor.jar), replace out/processor with processor.jar on -cp and -processorpath.

----

3) Using Maven (annotation-processor module)
A simple two-module approach or single-module with maven-compiler-plugin configuration.

Example single-module pom.xml snippets (place in project-root/pom.xml). Key parts only:

- Add processor to project's sources (as above).
- Configure maven-compiler-plugin to enable annotation processing and to run your processor:

```xml
<project>
  ...
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
        <configuration>
          <source>17</source>
          <target>17</target>
          <annotationProcessorPaths>
            <!-- If you built the processor into a separate artifact, list it here.
                 For same-module processing this is optional. -->
          </annotationProcessorPaths>
          <compilerArgs>
            <arg>-processor</arg>
            <arg>com.example.processor.LocalVarProcessor</arg>
          </compilerArgs>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- Run:
mvn -X compile

You should see the processor's notes in the Maven build output (compiler messages). If you prefer to package the processor separately:
- Build processor as its own module/artifact and add it to annotationProcessorPaths with groupId/artifactId/version.

----

4) Troubleshooting
- Use JDK javac (com.sun.* APIs require a JDK).
- If processor not discovered, ensure @SupportedAnnotationTypes("*") or list the annotation canonical name.
- RetentionPolicy.SOURCE is fine for compile-time processing.
- To see messages, set compiler verbosity or run with -X for Maven.
- If using modular JDK (modules), add --add-exports if access problems occur for com.sun.source packages on newer JDKs (rare for standard annotation processing use).

If you want, I can provide a complete pom.xml and the full LocalVarProcessor source file in one response. Which would you prefer?
 */