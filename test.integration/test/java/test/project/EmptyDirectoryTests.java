package test.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Base;
import com.github.sormuras.bach.Options;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmptyDirectoryTests {

  @Test
  void build(@TempDir Path temp) {
    var lines = new ArrayList<String>();
    var bach = new Bach(Options.of("--verbose")) {

      @Override
      protected Base newBase() {
        return Base.of(temp);
      }

      @Override
      public String print(String format, Object... args) {
        var line = String.format(format, args);
        lines.add(line);
        return line;
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
