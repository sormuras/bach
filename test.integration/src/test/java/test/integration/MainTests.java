package test.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Main;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.project.DeclaredModule;
import com.github.sormuras.bach.project.ExternalTool;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MainTests {
  @Test
  void modules() {
    var project = bach().project();
    assertLinesMatch(
        """
        com.github.sormuras.bach
        com.github.sormuras.bach
        test.base
        test.integration
        """
            .lines(),
        project.modules().stream().map(DeclaredModule::name).sorted());
    assertEquals(
        Set.of("com.github.sormuras.hello", "org.junit.platform.console"),
        project.externals().requires());
    assertLinesMatch(
        """
        format@.+""".lines(),
        project.externals().tools().stream().map(ExternalTool::name).sorted());
  }

  @Test
  void info() {
    var bach = bach();
    bach.run("info");
    assertLinesMatch(
        """
        info
        >>>>
        Tools
        >>>>
                    bach -> com.github.sormuras.bach/bach [Main]
        >> ... >>
        """.lines(),
        bach.configuration().printer().out().toString().lines(),
        bach.configuration().printer().toString());
  }

  static Bach bach(String... args) {
    return bach(ToolCall.of("bach").with(Stream.of(args)));
  }

  static Bach bach(ToolCall call) {
    return Main.bach(Printer.ofSilence(), call.arguments().toArray(String[]::new));
  }
}
