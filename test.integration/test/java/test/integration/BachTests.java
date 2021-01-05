package test.integration;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Base;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Recording;
import java.util.List;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;

class BachTests {
  @Test
  void defaults() {
    var bach = new Bach();
    assertNotNull(bach.toString());
  }

  @Test
  void print() {
    var bach = new Bach(Base.ofSystem(), silent -> {});

    bach.run(Command.of("print").add("10", "PRINT 'HELLO WORLD'"));
    bach.run(ToolProvider.findFirst("print").orElseThrow(), List.of("20", "PRINT 20"));
    bach.run(new PrintToolProvider("30 GOTO 10"));
    bach.run(new PrintToolProvider(true, "END.", 0));

    assertLinesMatch(
        """
            10 PRINT 'HELLO WORLD'
            20 PRINT 20
            30 GOTO 10
            END.
            """
            .lines(),
        bach.recordings().stream().map(Recording::output));
  }
}
