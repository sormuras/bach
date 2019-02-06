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

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void demo_00_bootstrap() throws Exception {
    var base = Path.of("demo", "00-bootstrap");
    var bach = new Bach(base, "clean", "build");
    var context = new BachContext(bach);
    var code = bach.run();
    assertEquals(0, code, context.bytesErr.toString());
    assertLinesMatch(
        List.of(
            "Bach - master - [" + base + "]",
            ">> FIXTURES >>",
            "Project '00-bootstrap 1.0.0-SNAPSHOT' build started...",
            ">> BUILD >>",
            "Project '00-bootstrap 1.0.0-SNAPSHOT' built successfully in .+ ms."),
        context.recorder.all);
  }

  @Test
  void demo_01_hello_world() throws Exception {
    var base = Path.of("demo", "01-hello-world");
    var bach = new Bach(base, "clean", "build");
    var context = new BachContext(bach);
    var code = bach.run();
    assertEquals(0, code, context.bytesErr.toString());
    assertLinesMatch(
        List.of(
            "Bach - master - [" + base + "]",
            ">> FIXTURES >>",
            "Project 'Hello World 1.0' build started...",
            ">> BUILD >>",
            "Project 'Hello World 1.0' built successfully in .+ ms."),
        context.recorder.all);
  }
}
