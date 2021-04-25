package test.integration.core;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Printer;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PrintTraitTests {

  @Nested
  class Modules {

    @Test
    void printModules() {
      var actual = new StringWriter();
      Bach.of(Printer.of(new PrintWriter(actual, true))).printModules();
      assertLinesMatch(
          """
          >>>>
          java\\.se.*
          >>>>
          jdk\\.net.*
          >>>>
            \\d+ modules
          """
              .lines(),
          actual.toString().lines());
    }

    @Test
    void printDeclaredModules() {
      var actual = new StringWriter();
      Bach.of(Printer.of(new PrintWriter(actual, true))).printDeclaredModules();
      assertLinesMatch(
          """
          TODO DECLARED
            0 modules
          """.lines(),
          actual.toString().lines());
    }

    @Test
    void printExternalModules() {
      var actual = new StringWriter();
      Bach.of(Printer.of(new PrintWriter(actual, true))).printExternalModules();
      assertLinesMatch(
          """
          >>>>
            \\d+ modules
          """.lines(),
          actual.toString().lines());
    }

    @Test
    void printSystemModules() {
      var actual = new StringWriter();
      Bach.of(Printer.of(new PrintWriter(actual, true))).printSystemModules();
      assertLinesMatch(
          """
          >>>>
          java\\.se.*
          >>>>
          jdk\\.net.*
          >>>>
            \\d+ modules
          """.lines(),
          actual.toString().lines());
    }

    @Test
    void printLayerModules() {
      var actual = new StringWriter();
      Bach.of(Printer.of(new PrintWriter(actual, true))).printLayerModules();
      assertLinesMatch(
          """
          >>>>
          java\\.se.*
          >>>>
          jdk\\.net.*
          >>>>
            \\d+ modules
          """.lines(),
          actual.toString().lines());
    }
  }

  @Nested
  class Tools {

    @Test
    void printTools() {
      var actual = new StringWriter();
      Bach.of(Printer.of(new PrintWriter(actual, true))).printTools();
      assertLinesMatch(
          """
          >>>>
          bach                 (EXTERNAL, com.github.sormuras.bach)
          >>>>
          javac                \\(SYSTEM, jdk\\.compiler.*\\)
          >>>>
          test                 (EXTERNAL, test.base)
          >>>>
            \\d+ tools
          """
              .lines(),
          actual.toString().lines());
    }
  }
}
