package scaffold.api;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.UnaryOperator;

public interface ScaffoldPlugin extends UnaryOperator<String> {

  static List<String> forEach(String string) {
    var list = new ArrayList<String>();
    for (var plugin : ServiceLoader.load(ScaffoldPlugin.class)) {
      list.add(plugin.apply(string));
    }
    return list;
  }
}
