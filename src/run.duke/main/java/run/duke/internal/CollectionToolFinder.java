package run.duke.internal;

import java.util.Collection;
import java.util.Optional;
import run.duke.Tool;
import run.duke.ToolFinder;

public record CollectionToolFinder(Optional<String> description, Collection<Tool> findTools)
    implements ToolFinder {}
