package run.duke.internal;

import java.util.Collection;
import run.duke.Tool;
import run.duke.ToolFinder;

public record CollectionToolFinder(String description, Collection<Tool> findTools)
    implements ToolFinder {
  public CollectionToolFinder {
    if (description == null) throw new IllegalArgumentException("description must not be null");
    if (description.isBlank()) throw new IllegalArgumentException("description must not be blank");
    if (findTools == null) throw new IllegalArgumentException("tool collection must not be null");
  }
}
