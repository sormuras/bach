package run.bach.internal.finder;

import java.util.List;
import run.bach.Tool;
import run.bach.ToolFinder;

public record ArrayToolFinder(String description, List<Tool> findAll) implements ToolFinder {}
