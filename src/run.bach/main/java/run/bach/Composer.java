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
import run.bach.tool.ImportTool;
import run.bach.tool.InfoTool;
import run.bach.tool.InstallTool;
import run.bach.tool.LaunchTool;
import run.bach.tool.LoadTool;
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
    var project = createProject();

    printer.log(Level.DEBUG, "Building browsing...");
    var browser = createBrowser();

    printer.log(Level.DEBUG, "Stuffing toolkit...");
    var toolkit = createToolkit();

    printer.log(Level.DEBUG, "Mapping workpieces...");
    var workpieces =
        new Workpieces()
            .put(Options.class, options)
            .put(Folders.class, folders)
            .put(Printer.class, printer)
            .put(Browser.class, browser)
            .put(Toolkit.class, toolkit)
            .put(Project.class, project);

    printer.log(Level.DEBUG, "Composing workbench...");
    return new Bach(workpieces, toolkit.toolbox());
  }

  public Project createProject() {
    var projectInfo = ProjectInfo.Support.of(getClass().getModule());
    return new Project(
        new Project.Name(options.projectName(projectInfo.name())),
        new Project.Version(options.projectVersionOrNow(), options.projectVersionTimestampOrNow()));
  }

  public ProjectTools createProjectTools() {
    return new ProjectTools();
  }

  public Browser createBrowser() {
    return new Browser();
  }

  public Toolkit createToolkit() {
    return new Toolkit(createToolbox(), createTweaks());
  }

  public Toolbox createToolbox() {
    var folders = Folders.CURRENT_WORKING_DIRECTORY;
    var externalModules = folders.externalModules();
    var externalTools = folders.externalTools();
    return Toolbox.compose(
        Toolbox.of(provideCommandTools()),
        Toolbox.ofModuleLayer(sourced), // .bach source modules and system modules
        Toolbox.ofModulePath(externalModules), // parent module layers are excluded
        Toolbox.ofJavaPrograms(externalTools, folders.javaHome("bin", "java")),
        Toolbox.of(provideProjectTools()),
        Toolbox.ofNativeToolsInJavaHome("java", "jcmd", "jfr"));
  }

  public Tweaks createTweaks() {
    return new Tweaks();
  }

  List<ToolOperator> provideDefaultToolOperators() {
    return List.of(
        new BuildTool(),
        new CacheTool(),
        new CleanTool(),
        new CompileTool(),
        new CompileClassesTool(),
        new CompileModulesTool(),
        new ImportTool(),
        new InfoTool(),
        new InstallTool(),
        new LaunchTool(),
        new LoadTool(),
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
    return Stream.concat(createProjectTools().stream(), provideDefaultToolOperators().stream())
        .map(Tool::of)
        .toList();
  }
}
