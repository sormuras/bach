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

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProjectTests {

  @Test
  void demo_01_hello_world() {
    var bach = new Bach(Path.of("demo", "01-hello-world"));
    var context = new BachContext(bach);
    Action.CLEAN.apply(bach);
    Action.BUILD.apply(bach);
    Assertions.assertLinesMatch(
        List.of("Building Hello World...", ">> BUILD LOG >>"), context.recorder.all);
  }
}
