import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CollectingLoggerFinder extends System.LoggerFinder {

  private final Map<String, CollectingLogger> loggers = new ConcurrentHashMap<>();

  @Override
  public System.Logger getLogger(String name, Module module) {
    var key = name + '@' + module;
    return loggers.computeIfAbsent(key, k -> new CollectingLogger(name));
  }
}
