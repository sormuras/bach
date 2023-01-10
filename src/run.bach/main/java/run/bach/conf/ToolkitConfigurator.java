package run.bach.conf;

import java.util.ArrayList;
import java.util.List;
import run.bach.Command;
import run.bach.Configurator;
import run.bach.Folders;
import run.bach.Setting;
import run.bach.Toolkit;
import run.bach.Workbench;
import run.bach.internal.ToolCallsToolOperator;
import run.duke.Tool;
import run.duke.ToolFinder;

public class ToolkitConfigurator implements Configurator {
  @Override
  public void configure(Workbench bench) {
    bench.put(new Toolkit(createToolFinder(bench)));
  }

  public ToolFinder createToolFinder(Workbench bench) {
    var folders = bench.getConstant(Folders.class);
    var setting = bench.getConstant(Setting.class);
    var externalModules = folders.externalModules();
    var externalTools = folders.externalTools();
    return ToolFinder.compose(
        ToolFinder.of(provideCommandTools(bench)),
        ToolFinder.ofModuleLayer(setting.layer()), // .bach source modules and system modules
        ToolFinder.ofModulePath(externalModules), // parent module layers are excluded
        ToolFinder.ofJavaPrograms(externalTools, folders.javaHome("bin", "java")),
        ToolFinder.ofNativeToolsInJavaHome("java", "jcmd", "jfr"));
  }

  List<Tool> provideCommandTools(Workbench bench) {
    var setting = bench.getConstant(Setting.class);
    var tools = new ArrayList<Tool>();
    var modules = new ArrayList<>(setting.layer().modules());
    modules.add(Command.class.getModule()); // "run.bach"
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
