//usr/bin/env jshell --execution local --show-version "$0" "$@"; exit $?

/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/open BUILDING
/open src/build/Build.java

var code = 0
try {
  Build.main();
} catch (Throwable throwable) {
  throwable.printStackTrace();
  code = 1;
}

if (code == 0) {
  run("javac", "-d", "target/build/main", "--module-source-path", "src/modules/*/main/java", "--module", "de.sormuras.bach");
  run("javac", "-d", "target/build/test", "--module-path", "target/build/main" + File.pathSeparator + "lib/test", "--module-source-path", "src/modules/*/test/java", "--module", "integration");
  exe("java", "--module-path", "target/build/test" + File.pathSeparator + "target/build/main" + File.pathSeparator + "lib/test", "--module", "integration/integration.IntegrationTests");
  exe("java", "--module-path", "target/build/test" + File.pathSeparator + "target/build/main" + File.pathSeparator + "lib/test" + File.pathSeparator + "lib/test-runtime-only", "--add-modules", "integration", "--module", "org.junit.platform.console", "--scan-modules");
}

/exit code
