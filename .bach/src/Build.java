/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bach's own build program.
 *
 * <p>Uses single-file source-code {@code Bach.java} to build module {@code de.sormuras.bach}.
 */
class Build {

  public static void main(String... args) {
    var bach =
        Bach.of(
            project ->
                project
                    .title("\uD83C\uDFBC Bach.java")
                    .version(Bach.VERSION)
                    .walkModuleInfoFiles(
                        List.of(
                            Path.of("src/de.sormuras.bach/main/java/module-info.java"),
                            Path.of("src/de.sormuras.bach/test/java-module/module-info.java"),
                            Path.of("src/test.base/test/java/module-info.java"),
                            Path.of("src/test.preview/test-preview/java/module-info.java")))
                    .tuner(Build::tune)
                    .library(
                        new Bach.Project.Library(
                            Set.of("org.junit.platform.console"), new Bach.ModulesMap()::get)));
    bach.build().assertSuccessful();
  }

  private static void tune(Bach.Call tool, Map<String, String> context) {
    if ("main".equals(context.get("realm"))) {
      if (tool instanceof Bach.Javac) {
        ((Bach.Javac) tool).setCompileForVirtualMachineVersion(11);
      }
      if (tool instanceof Bach.Javadoc) {
        tool.getAdditionalArguments()
            .add("-Xdoclint:-missing")
            .add("-link", "https://docs.oracle.com/en/java/javase/11/docs/api");
      }
    }
  }
}
