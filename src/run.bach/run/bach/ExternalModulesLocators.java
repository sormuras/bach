package run.bach;

import java.util.List;
import java.util.StringJoiner;

public record ExternalModulesLocators(List<ExternalModulesLocator> list) {
  public ExternalModulesLocators(List<ExternalModulesLocator> list) {
    this.list = List.copyOf(list);
  }

  public String toString(int indent) {
    var joiner = new StringJoiner("\n");
    list.forEach(locator -> joiner.add(locator.description()));
    joiner.add("    %d locator%s".formatted(list.size(), list.size() == 1 ? "" : "s"));
    return joiner.toString().indent(indent).stripTrailing();
  }
}
