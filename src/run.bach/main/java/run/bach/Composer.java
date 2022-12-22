package run.bach;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import run.bach.internal.PathSupport;
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
    var info = provideProjectInfo();
    return new Project(
        createProjectName(info),
        createProjectVersion(info),
        createProjectSpaces(info),
        new Project.Externals());
  }

  Project.Name createProjectName(ProjectInfo info) {
    var name = options.projectName(info.name());
    return new Project.Name(name.equals("*") ? PathSupport.name(folders.root(), "?") : name);
  }

  Project.Version createProjectVersion(ProjectInfo info) {
    var version = options.projectVersion(info.version());
    if (version.equals("*") || version.equalsIgnoreCase("now")) {
      var now = ZonedDateTime.now();
      var year = now.getYear();
      var month = now.getMonthValue();
      var day = now.getDayOfMonth();
      return new Project.Version(String.format("%4d.%02d.%02d-ea", year, month, day), now);
    }
    return new Project.Version(version, options.projectVersionTimestampOrNow());
  }

  Project.Spaces createProjectSpaces(ProjectInfo info) {
    var spaces = new ArrayList<Project.Space>();
    for (var space : info.spaces()) {
      var modules = new ArrayList<Project.DeclaredModule>();
      for (var module : space.modules()) {
        var root =
            info.moduleContentRootPattern()
                .replace("${space}", space.name())
                .replace("${module}", module);
        var unit =
            info.moduleContentInfoPattern()
                .replace("${space}", space.name())
                .replace("${module}", module);
        var content = Path.of(root);
        modules.add(new Project.DeclaredModule(content, content.resolve(unit)));
      }
      spaces.add(
          new Project.Space(
              space.name(),
              List.of(space.requires()),
              space.release(),
              List.of(space.launchers()),
              new Project.DeclaredModules(modules)));
    }
    return new Project.Spaces(List.copyOf(spaces));
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

  ProjectInfo provideProjectInfo() {
    var annotations =
        sourced.modules().stream()
            .filter(module -> module.isAnnotationPresent(ProjectInfo.class))
            .map(module -> module.getAnnotation(ProjectInfo.class))
            .toList();
    if (annotations.isEmpty()) return Bach.class.getModule().getAnnotation(ProjectInfo.class);
    if (annotations.size() > 1) throw new AssertionError("Too many @ProjectInfo found");
    return annotations.get(0);
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
