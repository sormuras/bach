package run.duke.internal;

import java.util.List;
import run.duke.Tool;
import run.duke.Toolbox;

public record ListToolbox(List<Tool> tools) implements Toolbox {}
