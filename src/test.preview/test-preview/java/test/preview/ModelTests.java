/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
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

package test.preview;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class ModelTests {

  @Test
  void build() {
    // var base = new Base(...)
    // var unit = new Unit("com.greetings", compile())
    // var main = new Realm("main", compile(), document(), image(), native(), package());
    // var test = new Realm("test", compile(), test());
    var main = new Realm("main", Task.of(__ -> {}));
    var test = new Realm("test", Task.of(__ -> {}));
    var project =
        new Project(
            "Demo",
            List.of(main, test),
            new Task(
                new BuildProject(),
                false,
                Task.of(new PrintProject()),
                Task.of(new VerifyProject()),
                Task.of(new CompileAllRealms()) //
                ) //
        );

    var bach = new Bach(project);
    bach.execute(project.build());
    assertLinesMatch(List.of("BuildProject", "PrintProject", "VerifyProject", "CompileAllRealms"), bach.log);
  }

  static class Bach {
    final List<String> log = new ArrayList<>();
    final Project project;

    Bach(Project project) {
      this.project = project;
    }

    void execute(Task task) {
      try {
        log.add(task.action().label());
        task.action().act(this);
        for (var sub : task.list()) execute(sub);
      } catch (Exception exception) {
        throw new Error("Task execution failed", exception);
      }
    }
  }

  record Project(String title, List<Realm> realms, Task build) {}

  record Realm(String name, Task task) {}

  @FunctionalInterface
  interface Action {
    void act(Bach bach) throws Exception;

    default String label() {
      return getClass().getSimpleName();
    }
  }

  record Task(Action action, boolean parallel, Task... list) {
    static Task of(Action action) {
      return new Task(action, false);
    }
  }

  static class BuildProject implements Action {

    @Override
    public void act(Bach bach) {
      System.out.println("Build project " + bach.project.title());
    }
  }

  static class PrintProject implements Action {

    @Override
    public void act(Bach bach) {
      var lines = List.of("Project", "\ttitle=" + bach.project.title());
      System.out.println(String.join(System.lineSeparator(), lines));
    }
  }

  static class VerifyProject implements Action {

    @Override
    public void act(Bach bach) {
      Objects.requireNonNull(bach.project.title());
      Objects.checkIndex(0, bach.project.realms.size());
    }
  }

  static class CompileAllRealms implements Action {
    @Override
    public void act(Bach bach) throws Exception {
    }
  }
}
