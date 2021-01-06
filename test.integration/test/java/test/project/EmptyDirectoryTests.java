package test.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Base;
import com.github.sormuras.bach.Flag;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmptyDirectoryTests {

  @Test
  void build(@TempDir Path temp) {
    var idea = Path.of(".idea/out/production");
    var base = Base.of(temp).cache(Files.isDirectory(idea) ? idea : Bach.CACHE);
    var lines = new ArrayList<String>();
    var bach = new Bach(base, lines::add, Flag.VERBOSE);

    var exception = assertThrows(IllegalStateException.class, bach::build);
    assertEquals("No modules found in file tree rooted at " + temp, exception.getMessage());

    assertLinesMatch(
        """
            // Build...
            // 0 module declaration(s) found
            No modules found in file tree rooted at %s
            """
            .formatted(temp)
            .lines()
            .toList(),
        lines);
  }
}
