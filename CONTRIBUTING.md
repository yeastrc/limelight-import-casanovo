Contributing / Development
==========================

Building and testing
--------------------

The build targets **Java 11** via a Gradle toolchain, so it compiles and tests to Java 11
regardless of the JDK that launches Gradle. Launch Gradle with any JDK that Gradle 8.10 supports
(**Java 11–23**); a Java 11 toolchain is auto-detected or provisioned. (The machine default `java`
may be newer than Gradle supports — set `JAVA_HOME` to an 11–21 JDK if `./gradlew` fails to start.)

```
./gradlew build        # compile, run tests, build the executable jar
./gradlew test         # run the unit and integration tests
./gradlew shadowJar    # build build/libs/casanovoToLimelightXML.jar
```

Code-coverage (JaCoCo) reports are written to `build/reports/jacoco/test/html`.

Strict linting
--------------

All Java is compiled with `-Xlint:all -Werror`; **any** compiler warning fails the build, for both
main and test sources. New code must be warning-clean.

Opt-in full-scale smoke test
----------------------------

The committed test fixtures are deliberately small. To exercise a **real, large** Casanovo run
end-to-end (e.g. tens of thousands of PSMs) without committing big files to the repository, run the
dedicated task with the input supplied via system properties:

```
./gradlew fullScaleSmokeTest \
    -Dcasanovo.smoke.mztab=/path/to/results.mztab \
    -Dcasanovo.smoke.config=/path/to/casanovo.yaml
```

It converts the file to a temporary output, validates it against the Limelight schema at full scale,
and checks that the PSM / scan-file / reported-peptide counts are consistent with the raw input. The
output (which can be hundreds of MB) is written to a temp file and deleted on exit. Without both
properties the task does nothing. The normal `./gradlew test` run excludes this test.
