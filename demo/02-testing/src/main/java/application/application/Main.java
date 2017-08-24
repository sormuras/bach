package application;

import application.api.*;
import java.util.Arrays;

public class Main {

  public static void main(String[] args) throws Exception {
    System.out.println("ApplicationVersion " + Version.VERSION);
    for (Plugin plugin : Plugin.load()) {
      System.out.println("ApplicationPlugin: " + plugin.getClass());
      apply(plugin, "123");
      apply(plugin, "abc");
      Arrays.stream(args).forEach(arg -> apply(plugin, arg));
    }
  }

  private static void apply(Plugin plugin, String input) {
    String name = plugin.getClass().getSimpleName();
    String output = plugin.apply(input);
    System.out.printf("'%s' -> [%s] -> '%s'%n", input, name, output);
  }
}
