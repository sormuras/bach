/*
 * Java Shell Builder
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
import java.util.logging.Level;

public class Demo {

  private static void basic() {
    Bach bach = new Bach();
    bach.set(Level.FINE);
    bach.set(Bach.Folder.SOURCE, Paths.get("demo/basic"));
    bach.set(Bach.Folder.TARGET, Paths.get("target/bach/demo/basic"));
    bach.call("java", "--version");
    bach.format();
    bach.compile();
    bach.run("com.greetings", "com.greetings.Main");
  }

  private static void common() {
    Bach bach = new Bach();
    bach.set(Level.FINE);
    bach.set(Bach.Folder.SOURCE, Paths.get("demo/common"));
    bach.set(Bach.Folder.TARGET, Paths.get("target/bach/demo/common"));
    bach.format();
    bach.compile();
    bach.run("com.greetings", "com.greetings.Main");
  }

  private static void idea() {
    Bach bach = new Bach();
    bach.set(Level.INFO);
    bach.set(Bach.Folder.SOURCE, Paths.get("demo/idea"));
    bach.set(Bach.Folder.TARGET, Paths.get("target/bach/demo/idea"));
    bach.format();
    bach.resolve(
        "org.junit.jupiter.api",
        "http://central.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.0.0-M4/junit-jupiter-api-5.0.0-M4.jar");
    bach.resolve(
        "org.junit.platform.commons",
        "http://central.maven.org/maven2/org/junit/platform/junit-platform-commons/1.0.0-M4/junit-platform-commons-1.0.0-M4.jar");
    bach.compile();
    bach.run("com.greetings", "com.greetings.Main");
    bach.call(
        "jdeps",
        "-profile",
        // "--dot-output", bach.path(Bach.Folder.TARGET),
        "--module-path",
        bach.path(Bach.Folder.TARGET_MAIN_COMPILE),
        "--module",
        "com.greetings");
    bach.javadoc();
    bach.test();
    // TODO bach.jar();
    // TODO bach.runJar("com.greetings");
    // TODO bach.link("com.greetings", "greetings");
  }

  public static void main(String... args) throws Exception {
    basic();
    System.out.println("\n\n");
    common();
    System.out.println("\n\n");
    idea();
  }
}
