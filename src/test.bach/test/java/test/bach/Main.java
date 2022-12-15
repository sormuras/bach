package test.bach;

import java.io.PrintWriter;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import jdk.jfr.Enabled;
import jdk.jfr.Registered;

public class Main implements ToolProvider {

  public static void main(String... args) {
    new Main().run(System.out, System.err, args);
  }

  @Override
  public String name() {
    return "test.bach";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    ServiceLoader.load(ToolProvider.class, Main.class.getClassLoader()).stream()
        .filter(provider -> provider.type().getModule() == Main.class.getModule())
        .filter(this::registered)
        .map(ServiceLoader.Provider::get)
        .filter(this::enabled)
        .forEach(this::run);
    return 0;
  }

  boolean registered(ServiceLoader.Provider<ToolProvider> provider) {
    var type = provider.type();
    var registered = type.getAnnotationsByType(Registered.class);
    return registered.length == 1 && registered[0].value();
  }

  boolean enabled(ToolProvider provider) {
    var type = provider.getClass();
    var enabled = type.getAnnotationsByType(Enabled.class);
    return enabled.length == 0 || enabled[0].value();
  }

  void run(ToolProvider provider) {
    System.out.println("| " + provider.name());
    provider.run(System.out, System.err);
  }
}
