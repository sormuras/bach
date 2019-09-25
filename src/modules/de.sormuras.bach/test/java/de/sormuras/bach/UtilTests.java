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

package de.sormuras.bach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.io.TempDir;

class UtilTests {
  @Test
  void listEmptyDirectoryYieldsAnEmptyListOfPaths(@TempDir Path temp) {
    assertEquals(List.of(), Util.list(temp));
    assertEquals(List.of(), Util.list(temp, Files::isRegularFile));
    assertEquals(List.of(), Util.list(temp, Files::isDirectory));
  }

  @Test
  void downloadFilesUsingFileScheme(@TempDir Path temp, TestReporter reporter) throws Exception {
    var a = Files.writeString(temp.resolve("a.txt"), "a");
    var b = Files.writeString(temp.resolve("b.txt"), "b");
    var c = Files.writeString(temp.resolve("c.txt"), "c");

    var download = temp.resolve("download");
    var a2 = download.resolve("a2.txt");
    var b2 = download.resolve("b2.txt");
    var c2 = download.resolve("c2.txt");

    var items =
        List.of(
            Util.Downloader.Item.of(a.toUri(), a2.getFileName().toString()),
            Util.Downloader.Item.of(b.toUri(), b2.getFileName().toString()),
            Util.Downloader.Item.of(c.toUri(), c2.getFileName().toString()));

    var out = new StringWriter();
    var err = new StringWriter();
    var transfer = new Util.Downloader(new PrintWriter(out), new PrintWriter(err));
    var paths = transfer.download(download, items);
    reporter.publishEntry("err", err.toString());

    assertEquals(Set.of(a2, b2, c2), paths);
    assertLinesMatch(Files.readAllLines(a), Files.readAllLines(a2));
    assertLinesMatch(Files.readAllLines(b), Files.readAllLines(b2));
    assertLinesMatch(Files.readAllLines(c), Files.readAllLines(c2));

    var d = Files.writeString(temp.resolve("d.txt"), "d");
    var d2 = download.resolve("d2.txt");
    transfer.download(d.toUri(), d2);
    assertLinesMatch(Files.readAllLines(d), Files.readAllLines(d2));
  }
}
