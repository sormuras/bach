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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BachContext.class)
class UtilTests {

  @Test
  void downloadUsingHttps(Bach bach) throws Exception {
    var temporary = Files.createTempDirectory("BachTests.downloadUsingHttps-");
    bach.util.download(URI.create("https://junit.org/junit5/index.html"), temporary);
    bach.util.removeTree(temporary);
  }

  @Test
  void downloadUsingLocalFileSystem(BachContext context) throws Exception {
    var bach = context.bach;
    var logger = context.recorder;

    var tempRoot = Files.createTempDirectory("BachTests.downloadUsingLocalFileSystem-");
    var content = List.of("Lorem", "ipsum", "dolor", "sit", "amet");
    var tempFile = Files.createFile(tempRoot.resolve("source.txt"));
    Files.write(tempFile, content);
    var tempPath = Files.createDirectory(tempRoot.resolve("target"));
    var first = bach.util.download(tempFile.toUri(), tempPath);
    var name = tempFile.getFileName().toString();
    var actual = tempPath.resolve(name);
    assertEquals(actual, first);
    assertTrue(Files.exists(actual));
    assertLinesMatch(content, Files.readAllLines(actual));
    assertLinesMatch(
        List.of(
            "download.*",
            "transferring `" + tempFile.toUri().toString() + "`...",
            "`" + name + "` downloaded .*"),
        logger.all);
    // reload
    logger.all.clear();
    var second = bach.util.download(tempFile.toUri(), tempPath);
    assertEquals(first, second);
    assertLinesMatch(
        List.of(
            "download.*",
            "local file already exists -- comparing properties to remote file...",
            "local and remote file properties seem to match, using .*"),
        logger.all);
    // offline mode
    logger.all.clear();
    bach.vars.offline = true;
    var third = bach.util.download(tempFile.toUri(), tempPath);
    assertEquals(second, third);
    assertLinesMatch(List.of("download.*"), logger.all);
    // offline mode with error
    Files.delete(actual);
    assertThrows(Error.class, () -> bach.util.download(tempFile.toUri(), tempPath));
    // online but different file
    logger.all.clear();
    bach.vars.offline = false;
    Files.write(actual, List.of("Hello world!"));
    var forth = bach.util.download(tempFile.toUri(), tempPath);
    assertEquals(actual, forth);
    assertLinesMatch(content, Files.readAllLines(actual));
    assertLinesMatch(
        List.of(
            "download.*",
            "local file already exists -- comparing properties to remote file...",
            "local file `.*` differs from remote one -- replacing it",
            "transferring `" + tempFile.toUri().toString() + "`...",
            "`" + name + "` downloaded .*"),
        logger.all);
    bach.util.removeTree(tempRoot);
  }

  @Test
  void isJavaFile(Bach.Utilities util) {
    assertFalse(util.isJavaFile(Path.of("")));
    assertFalse(util.isJavaFile(Path.of("a/b")));
    assertTrue(util.isJavaFile(Path.of("src/test/UtilTests.java")));
    assertFalse(util.isJavaFile(Path.of("src/test-resources/Util.isJavaFile.java")));
  }
}
