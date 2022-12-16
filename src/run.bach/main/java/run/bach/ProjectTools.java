package run.bach;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public record ProjectTools(List<ProjectTool> list) implements Iterable<ProjectTool> {
  public ProjectTools(ProjectTool... tools) {
    this(List.of(tools));
  }

  @Override
  public Iterator<ProjectTool> iterator() {
    return list.iterator();
  }

  public Stream<ProjectTool> stream() {
    return list.stream();
  }
}
