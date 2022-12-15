package run.bach;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import run.bach.internal.ToolCallsToolOperator;
import run.bach.tool.BuildTool;
import run.bach.tool.CacheTool;
import run.bach.tool.CleanTool;
import run.bach.tool.CompileClassesTool;
import run.bach.tool.CompileModulesTool;
import run.bach.tool.CompileTool;
import run.bach.tool.InfoTool;
import run.bach.tool.LaunchTool;
import run.bach.tool.TestTool;
import run.duke.Tool;
import run.duke.ToolCalls;
import run.duke.ToolOperator;
import run.duke.Toolbox;
import run.duke.Workbench;
import run.duke.Workpieces;

public class Composer {
  protected /*lazy*/ Options options;
  protected /*lazy*/ Folders folders;
  protected /*lazy*/ Printer printer;
  protected /*lazy*/ ModuleLayer sourced;

  public Composer() {
    this.options = Options.DEFAULTS;
    this.folders = Folders.CURRENT_WORKING_DIRECTORY;
    this.printer = Printer.BROKEN;
    this.sourced = ModuleLayer.empty();
  }

  Composer init(Options options, Folders folders, Printer printer, ModuleLayer sourced) {
    this.options = options;
    this.folders = folders;
    this.printer = printer;
    this.sourced = sourced;
    return this;
  }

  Workbench compose() {
    printer.log(Level.DEBUG, "Creating project model instance...");
    var project = composeProject();

    var workpieces =
        new Workpieces()
            .put(Options.class, options)
            .put(Folders.class, folders)
            .put(Printer.class, printer)
            .put(Browser.class, new Browser())
            .put(Project.class, project);

    printer.log(Level.DEBUG, "Stuffing toolbox...");
    var toolbox = composeToolbox();

    return new Bach(workpieces, toolbox);
  }

  public Project composeProject() {
    return Project.UNNAMED;
  }

  public List<ToolOperator> composeOperators() {
    return List.of();
  }

  Toolbox composeToolbox() {
    var folders = Folders.CURRENT_WORKING_DIRECTORY;
    var externalModules = folders.externalModules();
    var externalTools = folders.externalTools();
    var java = folders.javaHome("bin", "java");
    return Toolbox.compose(
        Toolbox.of(provideCommandTools()),
        Toolbox.ofModuleLayer(sourced), // .bach source modules and system modules
        Toolbox.ofModulePath(externalModules), // parent module layers are excluded
        Toolbox.ofJavaPrograms(externalTools, java),
        Toolbox.of(provideProjectTools()),
        Toolbox.ofNativeToolsInJavaHome("java", "jfr", "jdeprscan"));
  }

  List<ToolOperator> defaultOperators() {
    return List.of(
        new BuildTool(),
        new CacheTool(),
        new CleanTool(),
        new CompileTool(),
        new CompileClassesTool(),
        new CompileModulesTool(),
        new InfoTool(),
        new LaunchTool(),
        new TestTool());
  }

  List<Tool> provideCommandTools() {
    var tools = new ArrayList<Tool>();
    var modules = new ArrayList<>(sourced.modules());
    modules.add(Bach.class.getModule()); // "run.bach"
    for (var module : modules) {
      for (var command : module.getAnnotationsByType(Command.class)) {
        var identifier = module.getName() + '/' + command.name();
        var calls = command.mode().apply(command.args());
        var operator = new ToolCallsToolOperator(new ToolCalls(calls));
        var tool = Tool.of(identifier, operator);
        tools.add(tool);
      }
    }
    return tools;
  }

  List<Tool> provideProjectTools() {
    return Stream.concat(composeOperators().stream(), defaultOperators().stream())
        .map(Tool::of)
        .toList();
  }
}
