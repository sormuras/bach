// default package

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.spi.ToolProvider;

public class /*class.name {*/ Runner /*} class.name*/ {

  public static void main(String... args) {
    var tools =
        Tools.builder()
            .with(ServiceLoader.load(ToolProvider.class))
            .with(MethodToolProvider.find(Runner.class))
            .with(new Sleep())
            .build();

    var runner = new Runner(tools);
    runner.run("javac", "--version");
    runner.run("printf", "format %s%n", "argument");
    runner.run("mkdir", Path.of("bin", "123").toString());
    /*main.block {}*/
    runner.run("sleep");
    runner.run("sleep", "1234");
  }

  final Tools tools;

  Runner(Tools tools) {
    this.tools = tools;
    tools.info(new PrintWriter(System.out));
  }

  /** Run named tool with optional arguments. */
  void run(String name, String... args) {
    var strings = args.length == 0 ? "" : '"' + String.join("\", \"", args) + '"';
    System.out.printf("| %s(%s)%n", name, strings);
    var tool = tools.get(name);
    int code = tool.run(System.out, System.err, args);
    if (code != 0) {
      throw new RuntimeException("Non-zero exit code: " + code);
    }
  }

  static class Tools {

    static Builder builder() {
      return new Builder();
    }

    static class Builder {

      final Map<String, ToolProvider> map = new TreeMap<>();

      Builder with(ToolProvider provider) {
        var old = map.putIfAbsent(provider.name(), provider);
        if (old != null) {
          System.out.printf("Tool '%s' re-mapped.%n", provider.name());
        }
        return this;
      }

      Builder with(Iterable<ToolProvider> providers) {
        providers.forEach(this::with);
        return this;
      }

      Builder with(ServiceLoader<ToolProvider> loader) {
        loader.stream().map(ServiceLoader.Provider::get).forEach(this::with);
        return this;
      }

      Tools build() {
        return new Tools(Collections.unmodifiableMap(map));
      }
    }

    final Map<String, ToolProvider> map;

    Tools(Map<String, ToolProvider> map) {
      this.map = map;
    }

    ToolProvider get(String name) {
      var tool = map.get(name);
      if (tool == null) {
        throw new NoSuchElementException("No such tool: " + name);
      }
      return tool;
    }

    void info(PrintWriter writer) {
      writer.printf("%s%n", this);
      for (var entry : map.entrySet()) {
        var name = entry.getKey();
        var tool = entry.getValue();
        writer.printf("  - %8s [%s] %s%n", name, info(tool), tool);
      }
      writer.flush();
    }

    static String info(Object object) {
      var module = object.getClass().getModule();
      if (module.isNamed()) {
        return "module " + module.getDescriptor().toNameAndVersion();
      }
      try {
        var uri = object.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
        return "classpath-entry " + Path.of(uri).getFileName();
      } catch (NullPointerException | URISyntaxException ignore) {
        return module.toString();
      }
    }
  }

  static class Sleep implements ToolProvider {

    @Override
    public String name() {
      return "sleep";
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      long millis = 123;
      try {
        millis = Long.parseLong(args[0]);
      } catch (Exception e) {
        // ignore
      }
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        Thread.interrupted();
      }
      return 0;
    }
  }

  static class MethodToolProvider implements ToolProvider {

    static final Class<?>[] TYPES = {PrintWriter.class, PrintWriter.class, String[].class};

    static Collection<ToolProvider> find(Class<?> declaringClass) {
      var collection = new ArrayList<ToolProvider>();
      for (var method : declaringClass.getMethods()) {
        if (Modifier.isStatic(method.getModifiers())) {
          if (Arrays.equals(TYPES, method.getParameterTypes())) {
            collection.add(new MethodToolProvider(null, method));
          }
        }
      }
      return collection;
    }

    final Object object;
    final Method method;

    MethodToolProvider(Object object, Method method) {
      this.object = object;
      this.method = method;
    }

    @Override
    public String name() {
      return method.getName();
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      try {
        var result = method.invoke(object, out, err, args);
        if (method.getReturnType() == int.class) {
          return (int) result;
        }
        return 0;
      } catch (ReflectiveOperationException e) {
        e.printStackTrace(err);
        return 1;
      }
    }
  }

  @SuppressWarnings("unused")
  public static int printf(PrintWriter out, PrintWriter err, String... args) {
    if (args.length == 0) {
      return 1;
    }
    Object[] varargs = Arrays.copyOfRange(args, 1, args.length);
    out.printf(args[0], varargs);
    return 0;
  }

  @SuppressWarnings("unused")
  public static int mkdir(PrintWriter out, PrintWriter err, String... args) {
    var directory = Path.of(args[0]);
    try {
      Files.createDirectories(directory);
      return 0;
    } catch (Exception e) {
      e.printStackTrace(err);
      return 1;
    }
  }
}
