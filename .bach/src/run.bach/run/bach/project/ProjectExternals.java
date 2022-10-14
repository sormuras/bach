package run.bach.project;

import java.util.Set;
import java.util.stream.Stream;

public record ProjectExternals(Set<String> requires) implements ProjectComponent {

  public ProjectExternals() {
    this(Set.of());
  }

  public ProjectExternals withRequires(String... modules) {
    var requires = Set.copyOf(Stream.concat(requires().stream(), Stream.of(modules)).toList());
    return new ProjectExternals(requires);
  }
}
