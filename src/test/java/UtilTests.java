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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UtilTests {

  @Test
  void download() throws IOException {
    List<String> expectedLines = List.of("Lorem", "ipsum", "dolor", "sit", "amet");
    Path tempFile = Files.createTempFile("download-", ".txt");
    Files.write(tempFile, expectedLines);
    Path tempPath = Files.createTempDirectory("download-");
    Bach.Util.download(tempFile.toUri(), tempPath);
    Path actual = tempPath.resolve(tempFile.getFileName().toString());
    Assertions.assertTrue(Files.exists(actual));
    Assertions.assertLinesMatch(expectedLines, Files.readAllLines(actual));
  }

  @Test
  void findJdkHome() {
    Assertions.assertNotNull(Bach.Util.findJdkHome());
    Assertions.assertTrue(Bach.Util.findJdkHome().isAbsolute());
  }
}
