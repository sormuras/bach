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
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class BachTests {

  @Test
  @Disabled("while not pushed")
  void bootstrap() throws IOException {
    // Path target = Files.createDirectories(Paths.get("target"));
    Path target = Files.createTempDirectory("bach-bootstrap-");
    URL context = new URL("https://raw.githubusercontent.com/sormuras/bach/master/src/bach/");
    for (Path script : Set.of(target.resolve("Bach.java"), target.resolve("Bach.jsh"))) {
      // if (Files.exists(script)) continue; // uncomment to preserve existing files
      try (InputStream stream = new URL(context, script.getFileName().toString()).openStream()) {
        Files.copy(stream, script, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        System.err.println("bootstrap('" + script + "') failed: " + e.toString());
      }
    }
  }
}
