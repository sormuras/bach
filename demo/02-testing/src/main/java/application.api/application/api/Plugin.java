package application.api;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.UnaryOperator;

public interface Plugin extends UnaryOperator<String> {

  static List<Plugin> load() {
    List<Plugin> plugins = new ArrayList<>();
    for (Plugin plugin : ServiceLoader.load(Plugin.class)) {
      plugins.add(plugin);
    }
    return plugins;
  }
}
