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

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

class TaskTests {

  private final List<String> actualLogLines = new CopyOnWriteArrayList<>();
  private final Bach bach = createBach(actualLogLines);

  private Bach createBach(List<String> lines) {
    var bach = new Bach();
    bach.debug = true;
    bach.quiet = false;
    bach.offline = false;
    bach.logger = lines::add;
    return bach;
  }

  @Test
  void compiler() {
    var project =
        Project.builder()
            .newModuleGroup("xxx")
            .moduleSourcePath(List.of(Paths.get(".bach")))
            .end()
            .build();
    var compiler = new Task.CompilerTask(bach, project);
    assertEquals(Integer.valueOf(2), compiler.get());
  }
}
