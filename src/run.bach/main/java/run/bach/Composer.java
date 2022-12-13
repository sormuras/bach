package run.bach;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.spi.ToolProvider;
import run.bach.tool.BuildTool;
import run.bach.tool.CacheTool;
import run.bach.tool.CleanTool;
import run.bach.tool.CompileClassesTool;
import run.bach.tool.CompileModulesTool;
import run.bach.tool.CompileTool;
import run.bach.tool.LaunchTool;
import run.bach.tool.TestTool;
import run.duke.DukeTool;
import run.duke.Tool;
import run.duke.ToolCalls;
import run.duke.ToolFinder;
import run.duke.ToolFinders;
import run.duke.ToolOperator;
import run.duke.ToolRunner;

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

  ProjectToolRunner compose() {
    printer.log(Level.DEBUG, "Creating project model instance...");
    var project = composeProject();

    printer.log(Level.DEBUG, "Stuffing toolbox...");
    var toolbox = composeToolbox();

    return new Bach(options, folders, printer, project, toolbox);
  }

  public Project composeProject() {
    return Project.UNNAMED;
  }

  public List<Operator> composeOperators() {
    return List.of();
  }

  Toolbox composeToolbox() {
    var folders = Folders.CURRENT_WORKING_DIRECTORY;
    var externalModules = folders.externalModules();
    var externalTools = folders.externalTools();
    var java = folders.javaHome("bin", "java");
    var finders =
        new ToolFinders()
            .with(ToolFinder.ofToolCalls("@Command Tools", provideCommandToolCalls()))
            .with(ToolFinder.ofTools("Project Tools", provideProjectTools()))
            .with(ServiceLoader.load(sourced, ToolFinder.class))
            .with(
                ToolFinder.ofToolProviders(
                    "Tool Provider Services", ServiceLoader.load(sourced, ToolProvider.class)))
            .with(
                ToolFinder.ofToolProviders(
                    "Tool Providers in " + externalModules.toUri(), externalModules))
            .with(
                ToolFinder.ofJavaPrograms(
                    "Java Programs in " + externalTools.toUri(), externalTools, java))
            .with(
                ToolFinder.ofNativeTools(
                    "Native Tools in java.home -> " + folders.javaHome().toUri(),
                    name -> "java.home/" + name, // ensure stable names with synthetic namespace
                    folders.javaHome("bin"),
                    List.of("java", "jfr", "jdeprscan")));
    return new Toolbox(sourced, finders);
  }

  List<Operator> provideDefaultOperators() {
    return List.of(
        Operator.of("duke", DukeTool::new),
        Operator.of("build", BuildTool::new),
        Operator.of("cache", CacheTool::new),
        Operator.of("clean", CleanTool::new),
        Operator.of("compile", CompileTool::new),
        Operator.of("compile-classes", CompileClassesTool::new),
        Operator.of("compile-modules", CompileModulesTool::new),
        Operator.of("launch", LaunchTool::new),
        Operator.of("test", TestTool::new));
  }

  List<ToolCalls> provideCommandToolCalls() {
    var commands = new ArrayList<ToolCalls>();
    var modules = new ArrayList<>(sourced.modules());
    modules.add(getClass().getModule()); // run.bach
    for (var module : modules) {
      for (var command : module.getAnnotationsByType(Command.class)) {
        var identifier = module.getName() + '/' + command.name();
        var calls = command.mode().apply(command.args());
        commands.add(new ToolCalls(identifier, calls));
      }
    }
    return commands;
  }

  List<? extends Tool> provideProjectTools() {
    var tools = new ArrayList<Tool>();
    for (var projectTool : composeOperators()) tools.add(Tool.of(projectTool));
    for (var projectTool : provideDefaultOperators()) tools.add(Tool.of(projectTool));
    return List.copyOf(tools);
  }

  public record Operator(String name, Function<ToolRunner, ToolProvider> factory)
      implements ToolOperator {
    public static Operator of(String name, Function<ProjectToolRunner, ToolProvider> factory) {
      return new Operator(name, runner -> factory.apply((ProjectToolRunner) runner));
    }

    @Override
    public ToolProvider provider(ToolRunner runner) {
      return factory.apply(runner);
    }
  }
}
