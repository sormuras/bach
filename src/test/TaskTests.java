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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BachContext.class)
class TaskTests {

  private final BachContext context;
  private final Bach bach;

  TaskTests(BachContext context) {
    this.context = context;
    this.bach = context.bach;
  }

  @Test
  void compiler() {
    var project =
        Project.builder()
            .target(Paths.get("target/test/task/compiler"))
            .newModuleGroup("01-hello-world")
            .moduleSourcePath(List.of(Paths.get("demo/01-hello-world/src")))
            .end()
            .build();
    var compiler = new Task.CompilerTask(bach, project);
    assertEquals(0, (int) compiler.get());
  }

  @Test
  void runner() throws Exception {
    var temp = Files.createTempDirectory("TaskTests-runner-");
    var project =
        Project.builder()
            .entryPoint("world", "com.greetings.Main")
            .target(temp)
            .newModuleGroup("01-hello-world")
            .moduleSourcePath(
                List.of(
                    Paths.get("demo/01-hello-world/src"),
                    Paths.get("demo/01-hello-world/src-de"),
                    Paths.get("demo/01-hello-world/src-fr")))
            .end()
            .build();
    var runner = new Task.RunnerTask(bach, project);
    var resultBeforeCompile = runner.get();
    assertEquals(1, resultBeforeCompile.intValue());
    assertTrue(context.bytes.toString().contains("world not found"));
    context.bytes.reset();
    var compiler = new Task.CompilerTask(bach, project);
    assertEquals(0, (int) compiler.get());
    context.bytes.reset();
    var resultAfterCompile = runner.get();
    assertEquals(0, resultAfterCompile.intValue());
    var output = context.bytes.toString();
    assertTrue(output.contains("Greetings from 01-hello-world demo!"));
    assertTrue(output.contains("'Hello world' from class hello.Hello in module hello"));
    assertTrue(output.contains("'Hallo Welt' from class hallo.Hallo in module hallo"));
    assertTrue(output.contains("'Salut monde' from class salut.Salut in module bonjour"));
    bach.util.removeTree(temp);
  }
}
