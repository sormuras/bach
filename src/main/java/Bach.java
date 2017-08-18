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

// default package

import java.nio.file.*;
import java.util.*;

// Bach.java
class Bach {

  static void generateJShellScript(Path directory, String name) throws Exception {
    Path file = directory.resolve(name);
    List<String> script =
        List.of(
            "void java(Object... args) { JdkTool.execute(\"java\", args); }",
            "void javac(Object... args) { JdkTool.execute(\"javac\", args); }");
    Files.createDirectories(directory);
    Files.write(file, script);
  }
}
