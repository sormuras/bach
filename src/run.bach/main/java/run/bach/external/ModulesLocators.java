package run.bach.external;

import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;
import run.bach.internal.PathSupport;

public record ModulesLocators(List<ModulesLocator> list) {
  public static ModulesLocators of(Path directory) {
    var list =
        PathSupport.list(directory, PathSupport::isPropertiesFile).stream()
            .map(ModulesLocator::ofProperties)
            .toList();
    return new ModulesLocators(list);
  }

  public ModulesLocators(List<ModulesLocator> list) {
    this.list = List.copyOf(list);
  }

  public String toString(int indent) {
    var joiner = new StringJoiner("\n");
    list.forEach(locator -> joiner.add(locator.description()));
    joiner.add("    %d locator%s".formatted(list.size(), list.size() == 1 ? "" : "s"));
    return joiner.toString().indent(indent).stripTrailing();
  }
}
