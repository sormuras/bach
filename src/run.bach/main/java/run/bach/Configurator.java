package run.bach;

import java.util.ArrayList;
import java.util.ServiceLoader;

@FunctionalInterface
public interface Configurator {
  void configure(Workbench bench);

  static void configure(Workbench bench, ModuleLayer layer) {
    var loader = ServiceLoader.load(layer, Configurator.class);
    var parents = new ArrayList<Configurator>();
    var layered = new ArrayList<Configurator>();
    for (var configurator : loader) {
      var list = configurator.getClass().getModule().getLayer() == layer ? layered : parents;
      list.add(configurator);
    }
    for (var configurator : parents) configurator.configure(bench);
    for (var configurator : layered) configurator.configure(bench);
  }
}
