package test.integration;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Main;
import com.github.sormuras.bach.Printer;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.project.DeclaredModule;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MainTests {
  @Test
  void modules() {
    assertLinesMatch(
        """
        com.github.sormuras.bach
        com.github.sormuras.bach
        test.base
        test.integration
        """
            .lines(),
        bach().project().modules().stream().map(DeclaredModule::name).sorted());
  }

  static Bach bach(String... args) {
    return bach(ToolCall.of("bach").with(Stream.of(args)));
  }

  static Bach bach(ToolCall call) {
    return Main.bach(Printer.ofSilence(), call.arguments().toArray(String[]::new));
  }
}
