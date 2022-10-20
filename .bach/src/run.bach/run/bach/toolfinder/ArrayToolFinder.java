package run.bach.toolfinder;

import java.util.List;
import run.bach.Tool;
import run.bach.ToolFinder;

public record ArrayToolFinder(String description, List<Tool> findAll) implements ToolFinder {}
