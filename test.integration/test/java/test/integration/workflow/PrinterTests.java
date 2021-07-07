package test.integration.workflow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
              Project.of("empty", "0"),
              Settings.newSettings()
                  .with(new FolderSettings(temp))
                  .with(new LogbookSettings(new PrintWriter(out), new PrintWriter(out), true)));

      bach.printer().printAllModules();

      assertLinesMatch(
          """
          Declared Modules in Project Space: main
            -
          Declared Modules in Project Space: test
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

  @Nested
  class PrintToolsTests {
    @Test
    void empty(@TempDir Path temp) {
      var out = new StringWriter();
      var bach =
          Bach.of(
              Project.of("empty", "0"),
              Settings.newSettings()
                  .with(new FolderSettings(temp))
                  .with(new LogbookSettings(new PrintWriter(out), new PrintWriter(out), true)));

      bach.printer().printTools();

      var message = out.toString();
      assertTrue(message.contains("jar"), message);
      assertTrue(message.contains("javac"), message);
      assertTrue(message.contains("javadoc"), message);
      assertTrue(message.contains("jlink"), message);
      assertTrue(message.contains("jpackage"), message);
      assertFalse(message.contains("junit"), message);
    }

    @Test
    void bach() {
      var out = new StringWriter();
      var bach =
          Bach.of(
              Project.of("empty", "0"),
              Settings.newSettings()
                  .with(new FolderSettings(Path.of("")))
                  .with(new LogbookSettings(new PrintWriter(out), new PrintWriter(out), true)));

      bach.printer().printTools();

      var message = out.toString();
      assertTrue(message.contains("jar"), message);
      assertTrue(message.contains("javac"), message);
      assertTrue(message.contains("javadoc"), message);
      assertTrue(message.contains("jlink"), message);
      assertTrue(message.contains("jpackage"), message);
      assertTrue(message.contains("junit"), message);
    }
  }
}
