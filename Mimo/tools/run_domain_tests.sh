#!/usr/bin/env bash
# Compiles app/src/main/.../domain + app/src/test with the Kotlin compiler bundled in Gradle and runs the JUnit tests.
# Works fully offline (no Android SDK needed) because the domain layer is plain Kotlin/JVM.
set -euo pipefail
GL="${GRADLE_HOME:-/opt/gradle-8.14.3}/lib"
OUT="${TMPDIR:-/tmp}/mimo-domain-build"; rm -rf "$OUT"; mkdir -p "$OUT"
KC_CP="$GL/kotlin-compiler-embeddable-2.0.21.jar:$GL/kotlin-stdlib-2.0.21.jar:$GL/kotlin-script-runtime-2.0.21.jar:$GL/kotlin-reflect-2.0.21.jar:$GL/kotlin-daemon-embeddable-2.0.21.jar:$GL/trove4j-1.0.20200330.jar:$GL/annotations-24.0.1.jar:$GL/kotlinx-coroutines-core-jvm-1.6.4.jar"
TEST_CP="$GL/junit-4.13.2.jar:$GL/hamcrest-core-1.3.jar:$GL/kotlin-stdlib-2.0.21.jar"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
java -cp "$KC_CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -jvm-target 17 \
  -cp "$TEST_CP" -d "$OUT" $(find "$ROOT/app/src/main/java/com/mimo/companion/domain" "$ROOT/app/src/test/java/com/mimo/companion" -name "*.kt")
CLASSES=$(cd "$OUT" && find . -name '*Test.class' | sed 's#^\./##; s#\.class$##; s#/#.#g')
java -cp "$OUT:$TEST_CP" org.junit.runner.JUnitCore $CLASSES
