package test.integration.workflows;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.settings.Folders;
import com.github.sormuras.bach.settings.Logbook;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PrinterTests {

  @Nested
  class PrintModulesTests {

    @Test
    void empty(@TempDir Path temp) {
      var out = new StringWriter();
      var log = Logbook.of(new PrintWriter(out), new PrintWriter(out), true);
      var bach =
          Bach.of(
              Project.newProject("empty", "0"),
              Settings.newSettings().with(Folders.of(temp)).with(log));

      bach.printer().printAllModules();

      assertLinesMatch(
          """
          Declared Modules
            -
          External Modules
            -
          System Modules
            java.base@.+
            >> MORE MODULES >>
            jdk.zipfs@.+
          """
              .lines(),
          out.toString().lines());
    }
  }
}
