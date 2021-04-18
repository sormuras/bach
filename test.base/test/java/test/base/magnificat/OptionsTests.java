package test.base.magnificat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import test.base.magnificat.api.Option;
import test.base.magnificat.api.ProjectInfo;

class OptionsTests {

  @Test
  void empty() {
    assertTrue(Options.ofCommandLineArguments().map().isEmpty());
  }

  @Test
  void ofDefaultsFactory() {
    var options = Options.ofAllDefaults();
    assertLinesMatch(
        """
        --cli-bach-root
          .
        --cli-bach-info-folder
          .bach
        --cli-bach-info-module
          bach.info
        --verbose
        --project-name
          noname
        --project-version
          0
        --action
          build
        """
            .lines(),
        lines(options));
  }

  @Test
  void ofExplicitAnnotationElementsFromDefaultAnnotation() {
    var info = Bach.class.getModule().getAnnotation(ProjectInfo.class);
    var options = Options.ofProjectInfoElements(info);
    assertLinesMatch(
        """
        --project-name
          noname
        --project-version
          0
        """
            .lines(),
        lines(options));
  }

  @ParameterizedTest
  @EnumSource(Option.class)
  void passFlagAsCommandLineArgument(Option option) {
    if (!option.isFlag()) return;
    var options = Options.ofCommandLineArguments(option.cli());
    assertTrue(options.is(option));
    assertEquals("true", options.get(option));
    assertEquals(List.of("true"), options.values(option));
  }

  static Stream<String> lines(Options options) {
    var lines = new ArrayList<String>();
    for (var entry : new TreeMap<>(options.map()).entrySet()) {
      var option = entry.getKey();
      if (option.isFlag()) {
        lines.add(option.cli());
        continue;
      }
      if (option.isRepeatable()) {
        for (var value : entry.getValue()) {
          lines.add(option.cli());
          lines.add("  " + value);
        }
        continue;
      }
      lines.add(option.cli());
      lines.add("  " + String.join(" ", entry.getValue()));
    }
    return lines.stream();
  }
}
