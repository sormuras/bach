// default package

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class MakeBatchTests {
  @Test
  void constructor() {
    var batch = new Make.Batch("name", Make.Batch.Mode.SEQUENTIAL, List.of());
    assertEquals("name", batch.name);
    assertEquals(0, batch.arguments.size());
    assertEquals(Make.Batch.Mode.SEQUENTIAL, batch.mode);
    assertEquals(0, batch.commands.size());
  }

  @Test
  void names() {
    var root =
        new Make.Batch(
            "root",
            Make.Batch.Mode.SEQUENTIAL,
            List.of(
                new Make.Command("a", List.of()),
                new Make.Batch(
                    "b",
                    Make.Batch.Mode.PARALLEL,
                    List.of(new Make.Command("b1", List.of()), new Make.Command("b2", List.of()))),
                new Make.Command("c", List.of())));

    assertLinesMatch(
        List.of("root/ (SEQUENTIAL, 3)", "a", "b/ (PARALLEL, 2)", "b1", "b2", "c"),
        names(new ArrayList<>(), root));
  }

  private static List<String> names(List<String> names, Make.Command command) {
    if (command instanceof Make.Batch) {
      var batch = (Make.Batch) command;
      names.add(String.format("%s/ (%s, %s)", batch.name, batch.mode, batch.commands.size()));
      batch.commands.forEach(child -> names(names, child));
    } else {
      names.add(command.name);
    }
    return names;
  }

  @Test
  void parallel() {
    var ids = new ConcurrentHashMap<Long, String>();
    var wait =
        new Make.Command("wait", List.of()) {
          public int run(Make.Log log) {
            var thread = Thread.currentThread();
            ids.put(thread.getId(), thread.getName());
            try {
              Thread.sleep(100);
            } catch (InterruptedException e) {
              return 1;
            }
            return 0;
          }
        };
    var waits = new Make.Batch("waits", Make.Batch.Mode.PARALLEL, List.of(wait, wait, wait));
    var writer = new StringWriter();
    assertTimeout(Duration.ofMillis(200), () -> new Make(Make.Log.of(writer)).run(waits));
    assertTrue(ids.size() >= 2);
    assertTrue(writer.toString().isEmpty());
  }
}
