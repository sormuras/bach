package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.api.Option;
import java.util.StringJoiner;
import java.util.function.Predicate;

public record HelpMessageBuilder(Predicate<Option> optionFilter) {

  public String build() {
    var options = computeHelpMessage(optionFilter);

    return """
        Usage: bach [OPTIONS] [ACTIONS...]
                 to execute one or more actions in sequence

        Actions may either be passed via their --action <ACTION> option at a random
        location or by their <ACTION> name at the end of the command-line arguments.

        OPTIONS include:

        {{OPTIONS}}
        """
        .replace("{{OPTIONS}}", options.indent(2).stripTrailing())
        .stripTrailing();
  }

  public static String computeHelpMessage(Predicate<Option> filter) {
    var options = new StringJoiner("\n");
    for (var option : Option.values()) {
      if (!filter.test(option)) continue;
      options.add(option.cli() + " [" + option.cardinality() + "]");
      options.add(option.helpMessage().indent(4).stripTrailing());
    }
    return options.toString();
  }
}
