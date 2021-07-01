package test.integration.workflow;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Project;
import com.github.sormuras.bach.Settings;
import com.github.sormuras.bach.Settings.FolderSettings;
import com.github.sormuras.bach.Settings.LogbookSettings;
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
      var bach =
          Bach.of(
              Project.newProject("empty", "0"),
              Settings.newSettings()
                  .with(new FolderSettings(temp))
                  .with(new LogbookSettings(new PrintWriter(out), new PrintWriter(out), true)));

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
