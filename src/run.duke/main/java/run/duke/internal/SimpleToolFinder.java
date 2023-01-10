package run.duke.internal;

import java.util.List;
import run.duke.Tool;
import run.duke.ToolFinder;

public record SimpleToolFinder(List<Tool> tools) implements ToolFinder {}
