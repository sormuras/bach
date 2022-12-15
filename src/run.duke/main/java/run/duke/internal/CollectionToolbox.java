package run.duke.internal;

import java.util.Collection;
import run.duke.Tool;
import run.duke.Toolbox;

public record CollectionToolbox(Collection<Tool> tools) implements Toolbox {}
