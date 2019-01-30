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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BachContext.class)
class ToolTests {

  @Nested
  class Download {

    @Test
    void https(Bach bach) throws Exception {
      var uri = URI.create("https://junit.org/junit5/index.html");
      var temp = Files.createTempDirectory("bach-UtilTests.downloadUsingHttps-");
      var path = new Tool.Download(uri, temp).run(bach);
      var text = Files.readString(path);
      assertTrue(text.contains("<title>JUnit 5</title>"));
      Util.removeTree(temp);
    }

    @Test
    void defaultFileSystem(BachContext context) throws Exception {
      var bach = context.bach;
      var logger = context.recorder;

      var tempRoot = Files.createTempDirectory("BachTests.downloadUsingLocalFileSystem-");
      assertEquals(1, new Tool.Download(URI.create("void"), tempRoot).apply(bach));
      assertTrue(context.bytesErr.toString().contains("URI is not absolute"));
      logger.all.clear();

      var content = List.of("Lorem", "ipsum", "dolor", "sit", "amet");
      var tempFile = Files.createFile(tempRoot.resolve("source.txt"));
      Files.write(tempFile, content);
      var tempPath = Files.createDirectory(tempRoot.resolve("target"));
      var first = new Tool.Download(tempFile.toUri(), tempPath).run(bach);
      var name = tempFile.getFileName().toString();
      var actual = tempPath.resolve(name);
      assertEquals(actual, first);
      assertTrue(Files.exists(actual));
      assertLinesMatch(content, Files.readAllLines(actual));
      assertLinesMatch(
          List.of(
              "[run] Download...",
              "download.*",
              "transferring `" + tempFile.toUri().toString() + "`...",
              "`" + name + "` downloaded .*",
              "[run] Download done."),
          logger.all);
      // reload
      logger.all.clear();
      var second = new Tool.Download(tempFile.toUri(), tempPath).run(bach);
      assertEquals(first, second);
      assertLinesMatch(
          List.of(
              "[run] Download...",
              "download.*",
              "local file already exists -- comparing properties to remote file...",
              "local and remote file properties seem to match, using .*",
              "[run] Download done."),
          logger.all);
      // offline mode
      logger.all.clear();
      bach.var.offline = true;
      var third = new Tool.Download(tempFile.toUri(), tempPath).run(bach);
      assertEquals(second, third);
      assertLinesMatch(
          List.of("[run] Download...", "download.*", "[run] Download done."), logger.all);
      // offline mode with error
      Files.delete(actual);
      assertThrows(Error.class, () -> new Tool.Download(tempFile.toUri(), tempPath).apply(bach));
      // online but different file
      logger.all.clear();
      bach.var.offline = false;
      Files.write(actual, List.of("Hello world!"));
      var forth = new Tool.Download(tempFile.toUri(), tempPath).run(bach);
      assertEquals(actual, forth);
      assertLinesMatch(content, Files.readAllLines(actual));
      assertLinesMatch(
          List.of(
              "[run] Download...",
              "download.*",
              "local file already exists -- comparing properties to remote file...",
              "local file `.*` differs from remote one -- replacing it",
              "transferring `" + tempFile.toUri().toString() + "`...",
              "`" + name + "` downloaded .*",
              "[run] Download done."),
          logger.all);
      Util.removeTree(tempRoot);
    }
  }

  @Nested
  class GoogleJavaFormat {

    @Test
    void formatAndCheck(Bach bach) {
      var format = new Tool.GoogleJavaFormat(true, List.of(Path.of("src")));
      assertEquals(0, format.run(bach));

      var verify = new Tool.GoogleJavaFormat(false, List.of(Path.of("src")));
      assertEquals(0, verify.run(bach));
    }
  }
}
