package test.integration.trait;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.StringWriter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import test.integration.Auxiliary;

class PrintTraitTests {

  @Nested
  class Modules {

    @Test
    void printModules() {
      var actual = new StringWriter();
      Auxiliary.newEmptyBach(actual).printModules();
      assertLinesMatch(
          """
          Declared Modules
            No modules declared by project empty
          External Modules
          >>>>
          System Modules
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
      Auxiliary.newEmptyBach(actual).printDeclaredModules();
      assertLinesMatch(
          """
          Declared Modules
            No modules declared by project empty
          """.lines(),
          actual.toString().lines());
    }

    @Test
    void printExternalModules() {
      var actual = new StringWriter();
      Auxiliary.newEmptyBach(actual).printExternalModules();
      assertLinesMatch(
          """
          External Modules
          >>>>
              \\d+ modules
          """.lines(),
          actual.toString().lines());
    }

    @Test
    void printSystemModules() {
      var actual = new StringWriter();
      Auxiliary.newEmptyBach(actual).printSystemModules();
      assertLinesMatch(
          """
          System Modules
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
  }

  @Nested
  class Tools {

    @Test
    void printTools() {
      var actual = new StringWriter();
      Auxiliary.newEmptyBach(actual).printTools();
      assertLinesMatch(
          """
          >>>>
          bach                 \\(EXTERNAL, com\\.github\\.sormuras\\.bach.*\\)
          >>>>
          javac                \\(SYSTEM, jdk\\.compiler.*\\)
          >>>>
            \\d+ tools
          """
              .lines(),
          actual.toString().lines());
    }

    @Test
    void printToolDescriptionFor123() {
      var actual = new StringWriter();
      Auxiliary.newEmptyBach(actual).printToolDescription("123");
      assertLinesMatch(
          """
          123 not found
          """.lines(), actual.toString().lines());
    }

    @Test
    void printToolDescriptionForBach() {
      var actual = new StringWriter();
      Auxiliary.newEmptyBach(actual).printToolDescription("bach");
      assertLinesMatch(
          """
          bach                 \\(EXTERNAL, com\\.github\\.sormuras\\.bach.*\\)
              Builds (on(ly)) Java Modules
              https://github.com/sormuras/bach
          """
              .lines(),
          actual.toString().lines());
    }
  }
}
