package test.base;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CollectingLoggerFinder extends System.LoggerFinder {

  private final Map<String, CollectingLogger> loggers = new ConcurrentHashMap<>();

  @Override
  public System.Logger getLogger(String name, Module module) {
    return loggers.computeIfAbsent(name, CollectingLogger::new);
  }
}
