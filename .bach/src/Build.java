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

/**
 * Bach's own build program.
 *
 * <p>Uses single-file source-code {@code Bach.java} to build module {@code de.sormuras.bach}.
 */
class Build {

  public static void main(String... args) {
    Bach.of(
            walker ->
                walker
                    .setWalkOffset("src")
                    .setWalkDepthLimit(5)
                    .setLayout(Bach.Walker.Layout.MAIN_TEST_PREVIEW),
            project ->
                project
                    .title("\uD83C\uDFBC Bach.java")
                    .version(Bach.VERSION.toString())
                    .requires("org.junit.platform.console"))
        .build(
            (arguments, project, map) -> {
              Bach.Sequencer.Tuner.defaults(arguments, project, map);
              var tool = map.get("tool");
              if ("main".equals(map.get("realm"))) {
                if ("javac".equals(tool)) arguments.put("--release", 11);
                if ("javadoc".equals(tool)) {
                  arguments.put("-Xdoclint:-missing");
                  arguments.add("-link", "https://docs.oracle.com/en/java/javase/11/docs/api");
                }
              }
              if ("test-preview".equals(map.get("realm"))) {
                if (tool.equals("javac")) arguments.put("-Xlint:-preview");
              }
            })
        .assertSuccessful();
  }
}
