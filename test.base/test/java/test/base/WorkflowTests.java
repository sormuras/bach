package test.base;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WorkflowTests {
  @Test
  void test() {
    var bach = new Bach(new ArrayList<>());
    try (bach) {
      bach.say("BEGIN");
      bach.run(
          new Sequence(
              __ -> __.say("a"),
              new Parallel(new Say("b 1"), __ -> __.say("b 2"), new Say("b 3")),
              __ -> __.say("c")));

      var project = new Project("Unnamed");
      bach.run(new Build(project));
      bach.say("END.");
    }

    assertLinesMatch(
        """
        [1] BEGIN
        [1] a
        \\[.+\\] b .
        \\[.+\\] b .
        \\[.+\\] b .
        [1] c
        [1] Build Project[name=Unnamed]
        [1] END.
        """
            .lines(),
        bach.lines().stream());
  }

  record Bach(List<String> lines) implements AutoCloseable {
    void say(String text) {
      lines.add("[" + Thread.currentThread().getId() + "] " + text);
    }

    void run(Workflow workflow) {
      try {
        workflow.run(this);
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public void close() {}
  }

  @FunctionalInterface
  interface Workflow {
    void run(Bach bach);
  }

  record Parallel(Workflow... workflows) implements Workflow {
    @Override
    public void run(Bach bach) {
      Stream.of(workflows).parallel().forEach(workflow -> workflow.run(bach));
    }
  }

  record Sequence(Workflow... workflows) implements Workflow {
    @Override
    public void run(Bach bach) {
      Stream.of(workflows).forEach(workflow -> workflow.run(bach));
    }
  }

  record Say(String text) implements Workflow {
    @Override
    public void run(Bach bach) {
      bach.say(text);
    }
  }

  record Project(String name) {}

  record Build(Project project) implements Workflow {
    @Override
    public void run(Bach bach) {
      bach.say("Build " + project);
    }
  }
}
