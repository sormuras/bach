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
    new Demo().jsb();
    new Demo().basic();
  }

  private void basic() {
    //    if (!Boolean.getBoolean("basic")) return;
    //    Bach bach =
    //        new Bach.Builder()
    //            .name("basic")
    //            .version("0.9")
    //            .level(Level.FINE)
    //            .folder(Bach.Folder.SOURCE, Paths.get("demo/basic"))
    //            .folder(Bach.Folder.TARGET, Paths.get("target/demo/basic"))
    //            .build();
    //    bach.build();
    //
    //    // run
    //    String module = "com.greetings";
    //    String main = "com.greetings.Main";
    //    Path modulePath = bach.path(Bach.Folder.TARGET_MAIN_COMPILE);
    //    bach.call("java", "--module-path", modulePath, "--module", module + "/" + main);
  }

  private void jsb() {
    StackWalker.getInstance().forEach(System.out::println);
    Bach builder = new Bach();
    builder.call("java", "--version");
    builder.javac(
        option -> {
          option.moduleSourcePaths =
              List.of(Paths.get("demo/basic/greetings"), Paths.get("demo/basic/hello"));
          option.destinationPath = Paths.get("target/jshell/demo/basic");
          return option;
        });
    builder.java(
        option -> {
          option.modulePaths = List.of(Paths.get("target/jshell/demo/basic"));
          option.module = "com.greetings/com.greetings.Main";
          return option;
        });
    StackWalker.getInstance().forEach(System.out::println);
  }
}
