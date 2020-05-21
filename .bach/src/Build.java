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
import java.util.Map;

/**
 * Bach's own build program.
 *
 * <p>Uses single-file source-code {@code Bach.java} to build module {@code de.sormuras.bach}.
 */
class Build {

  public static void main(String... args) {
    var project =
        new Bach.Walker()
            .setWalkOffset(Path.of("src"))
            .setWalkDepthLimit(5)
            .setLayout(Bach.Walker.Layout.MAIN_TEST_PREVIEW)
            .newBuilder()
            .title("\uD83C\uDFBC Bach.java")
            .version(Bach.VERSION.toString())
            .requires("org.junit.platform.console")
            .newProject();
    Bach.of(project).build(Build::tune).assertSuccessful();
  }

  private static void tune(Bach.Sequencer.Arguments arguments, Map<String, String> context) {
    if ("main".equals(context.get("realm"))) {
      switch (context.get("tool")) {
        case "javac":
          arguments.put("--release", 11);
          break;
        case "javadoc":
          arguments
              .put("-Xdoclint:-missing")
              .add("-link", "https://docs.oracle.com/en/java/javase/11/docs/api");
          break;
      }
    }
  }
}
