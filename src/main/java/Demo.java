/*
 * Bach - Java Shell Builder
 * Copyright (C) 2017 Christian Stein
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

import java.nio.file.Paths;
import java.util.List;

class Demo {

  public static void main(String... args) throws Exception {
    new Demo().basic();
    new Demo().common();
  }

  private void basic() {
    StackWalker.getInstance().forEach(System.out::println);
    Bach bach = new Bach();
    bach.call("java", "--version");
    bach.javac(
        option -> {
          option.moduleSourcePaths =
              List.of(Paths.get("demo/basic/greetings"), Paths.get("demo/basic/hello"));
          option.destinationPath = Paths.get("target/jshell/demo/basic");
          return option;
        });
    bach.java(
        option -> {
          option.modulePaths = List.of(Paths.get("target/jshell/demo/basic"));
          option.module = "com.greetings/com.greetings.Main";
          return option;
        });
    StackWalker.getInstance().forEach(System.out::println);
  }

  private void common() {
    Bach bach = new Bach();
    bach.javac(
        options -> {
          options.moduleSourcePaths = List.of(Paths.get("deprecated/demo/common/main/java"));
          options.destinationPath = Paths.get("target/jshell/demo/common");
          return options;
        });
    bach.java(
        option -> {
          option.modulePaths = List.of(Paths.get("target/jshell/demo/common"));
          option.module = "com.greetings/com.greetings.Main";
          return option;
        });
  }
}
