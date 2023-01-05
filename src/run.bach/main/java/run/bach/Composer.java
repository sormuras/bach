package run.bach;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import run.bach.internal.ToolCallsToolOperator;
import run.duke.Tool;
import run.duke.Toolbox;
import run.duke.Workbench;

public class Composer {
  private volatile Components components;

  public Composer() {}

  public final Browser browser() {
    return components.get(Browser.class);
  }

  public final Options options() {
    return components.get(Options.class);
  }

  public final Folders folders() {
    return components.get(Folders.class);
  }

  public final Printer printer() {
    return components.get(Printer.class);
  }

  public final Project project() {
    return components.get(Project.class);
  }

  public final Setting setting() {
    return components.get(Setting.class);
  }

  public final Toolkit toolkit() {
    return components.get(Toolkit.class);
  }

  Bach composeBach(Components components) {
    this.components = components;
    var printer = printer();

    printer.log(Level.DEBUG, "Creating project model instance...");
    components.put(Project.class, createProject());

    printer.log(Level.DEBUG, "Building browser...");
    components.put(Browser.class, createBrowser());

    printer.log(Level.DEBUG, "Stuffing toolkit...");
    var toolkit = createToolkit();
    components.put(Toolkit.class, toolkit);
    components.put(Toolbox.class, toolkit.toolbox());

    printer.log(Level.DEBUG, "Composing workbench...");
    var workbench = Workbench.of(setting().layer());
    return new Bach(browser(), folders(), options(), printer(), project(), toolkit(), workbench);
  }

  public Project createProject() {
    return findProjectInfoAnnotation()
        .map(info -> (ProjectFactory) new ProjectFactory.OfProjectInfo(this, info))
        .orElseGet(() -> new ProjectFactory.OfConventions(this))
        .createProject();
  }

  public Browser createBrowser() {
    return new Browser();
  }

  public Toolkit createToolkit() {
    return new Toolkit(createToolbox(), createTweaks());
  }

  public Toolbox createToolbox() {
    var externalModules = folders().externalModules();
    var externalTools = folders().externalTools();
    return Toolbox.compose(
        Toolbox.of(provideCommandTools()),
        Toolbox.ofModuleLayer(setting().layer()), // .bach source modules and system modules
        Toolbox.ofModulePath(externalModules), // parent module layers are excluded
        Toolbox.ofJavaPrograms(externalTools, folders().javaHome("bin", "java")),
        Toolbox.ofNativeToolsInJavaHome("java", "jcmd", "jfr"));
  }

  public Tweaks createTweaks() {
    return new Tweaks();
  }

  Optional<ProjectInfo> findProjectInfoAnnotation() {
    var annotations =
        setting().layer().modules().stream()
            .filter(module -> module.isAnnotationPresent(ProjectInfo.class))
            .map(module -> module.getAnnotation(ProjectInfo.class))
            .toList();
    if (annotations.isEmpty()) return Optional.empty();
    if (annotations.size() > 1) throw new AssertionError("Too many @ProjectInfo found");
    return Optional.of(annotations.get(0));
  }

  List<Tool> provideCommandTools() {
    var tools = new ArrayList<Tool>();
    var modules = new ArrayList<>(setting().layer().modules());
    modules.add(Bach.class.getModule()); // "run.bach"
    for (var module : modules) {
      for (var command : module.getAnnotationsByType(Command.class)) {
        var identifier = module.getName() + '/' + command.name();
        var calls = command.mode().apply(command.args());
        var operator = new ToolCallsToolOperator(calls);
        var tool = Tool.of(identifier, operator);
        tools.add(tool);
      }
    }
    return tools;
  }
}
