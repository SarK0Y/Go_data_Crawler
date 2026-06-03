/*
 <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-shade-plugin</artifactId>
      <version>3.5.0</version>
      <executions>
        <execution>
          <phase>package</phase>
          <goals>
            <goal>shade</goal>
          </goals>
          <configuration>
            <transformers>
              <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                <mainClass>com.example.Main</mainClass>
              </transformer>
            </transformers>
          </configuration>
        </execution>
      </executions>
    </plugin>
 */
/*
Maven Shade advanced: Custom resource transformers, relocation to avoid dependency conflicts.

Gradle Shadow tricks: Merging service files, handling multiple entry points.

JAR signing basics: Understanding why those META‑INF signatures exist in the first place.

Java packaging best practices: Alternatives like modular JARs or Docker images for deployment.
 */