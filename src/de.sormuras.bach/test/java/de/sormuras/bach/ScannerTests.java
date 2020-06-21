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

package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import de.sormuras.bach.project.Base;
import java.io.File;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScannerTests {

  @Test
  void defaults() {
    var logs = new ArrayList<String>();
    var logbook = new Logbook(logs::add, Level.ALL);
    var base = Base.of("doc", "project", "JigsawQuickStart");
    var files = Scanner.findModuleInfoJavaFiles(base, Path.of(""), 3);
    var scanner = new Scanner(logbook, base, Scanner.Layout.AUTOMATIC, files);
    var project = scanner.scan();
    assertLinesMatch(List.of(">> SCAN DIRECTORY >>"), logs);
    assertEquals("JigsawQuickStart 1-ea", project.toNameAndVersion());
    assertLinesMatch(List.of("project JigsawQuickStart {", ">>>>", "}"), project.toStrings());
    assertLinesMatch(
        List.of(
            "-d",
            base.classes("", Runtime.version().feature()).toString(),
            "--module",
            "com.greetings",
            "--module-version",
            "1-ea",
            "--module-source-path",
            base.directory().toString()),
        project.main().javac().toStrings());
    assertLinesMatch(
        List.of(
            "--create",
            "--file",
            base.modules("").resolve("com.greetings@1-ea.jar").toString(),
            "-C",
            base.classes("", Runtime.version().feature()) + File.separator + "com.greetings",
            "."),
        project.main().unit("com.greetings").orElseThrow().jar().toStrings());
  }
}
