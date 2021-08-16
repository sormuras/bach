package test.base;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static test.base.WorkflowTests.Bach.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WorkflowTests {
  @Test
  void test() {
    say("BEGIN");
    run(
        () -> say("a"),
        new Parallel(new Say("b 1"), () -> say("b 2"), new Say("b 3")),
        () -> say("c"));

    var project = new ProjectWither(new Project("Unnamed")).withName("Noname").project();
    run(new CompileWorkflow(project), new DocumentWorkflow(project));
    say("END.");

    assertLinesMatch(
        """
         1: BEGIN
         1: a
        .+: b .
        .+: b .
        .+: b .
         1: c
        TODO Write lines to disk
         1: Compile Project[name=Noname]
         1: Generate Noname's API documentation
        TODO Write lines to disk
         1: END.
        """
            .lines(),
        Bach.lines.stream());
  }

  record Bach() {

    static List<String> lines = new ArrayList<>();
    static Deque<Runnable> runnables = new ArrayDeque<>();

    static void say(String text) {
      var thread = Thread.currentThread().getId();
      lines.add("%2s: %s".formatted(thread, text));
    }

    static void run(Runnable... runnables) {
      run(new Sequence(runnables));
    }

    static void run(Runnable runnable) {
      runnables.push(runnable);
      try {
        runnable.run();
      } catch (Exception e) {
        throw new IllegalStateException(e);
      } finally {
        runnables.pop();
        if (runnables.isEmpty()) {
          lines.add("TODO Write lines to disk");
        }
      }
    }
  }

  record Parallel(Runnable... runnables) implements Runnable {
    @Override
    public void run() {
      Stream.of(runnables).parallel().forEach(Runnable::run);
    }
  }

  record Sequence(Runnable... runnables) implements Runnable {
    @Override
    public void run() {
      Stream.of(runnables).forEach(Runnable::run);
    }
  }

  record Say(String text) implements Runnable {
    @Override
    public void run() {
      Bach.say(text);
    }
  }

  record Project(String name) {}

  record ProjectWither(Project project) {
    ProjectWither withName(String name) {
      return new ProjectWither(new Project(name));
    }
  }

  record CompileWorkflow(Project project) implements Runnable {
    @Override
    public void run() {
      Bach.say("Compile " + project);
    }
  }

  record DocumentWorkflow(Project project) implements Runnable {
    @Override
    public void run() {
      Bach.say("Generate %s's API documentation".formatted(project.name));
    }
  }
}
