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

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildTests {

  @Test
  void buildBach(@TempDir Path work) {
    var home = Path.of("");
    var test = new TestRun(home, work);
    var bach = new Bach(test);

    assertLinesMatch(
        List.of(
            "Bach (master) initialized for bach " + Bach.VERSION,
            "  name = bach",
            "  version = " + Bach.VERSION,
            ">> RUN TRACE LINES >>"),
        test.outLines());

    try {
      bach.run(List.of(new JigsawBuilder()));
    } catch (Exception e) {
      // ignore
    }

    assertLinesMatch(List.of(), test.errLines());
    assertLinesMatch(List.of(
        ">> INIT >>",
        "Performing 1 action(s)...",
        ">> BUILD >>",
        "1 action(s) successfully performed."), test.outLines());
  }
}
