package test.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Base;
import com.github.sormuras.bach.Flag;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmptyDirectoryTests {

  @Test
  void build(@TempDir Path temp) {
    var lines = new ArrayList<String>();
    var bach = new Bach() {

      @Override
      protected Base newBase() {
        var idea = Path.of(".idea/out/production");
        return Base.of(temp); // .cache(Files.isDirectory(idea) ? idea : Bach.CACHE);
      }

      @Override
      protected Set<Flag> newFlags() {
        return EnumSet.of(Flag.VERBOSE);
      }

      @Override
      protected Consumer<String> newPrinter() {
        return lines::add;
      }
    };

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
