import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolFinder;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Comparator;
import java.util.List;
import java.util.spi.ToolProvider;

class describe {
  public static void main(String... args) {
    var arguments = java.util.List.of(args);
    if (arguments.isEmpty()) {
      System.out.println();
      System.out.printf("# Describe Bach %s%n", Bach.version());
      System.out.println();
      System.out.printf("  user.dir = `%s`%n", System.getProperty("user.dir"));
    }

    var bach = new Bach();

    if (arguments.isEmpty() || arguments.contains("modules")) {
      System.out.println();
      System.out.println("## Modules");
      //      System.out.println();
      //      System.out.println(bach.explorer().describeDeclaredModules());
      //      System.out.println();
      //      System.out.println(bach.explorer().describeExternalModules());
      System.out.println();
      System.out.println("### System Modules");
      System.out.println();
      list(ModuleFinder.ofSystem()).stream().map(describe::item).forEach(System.out::println);
    }

    if (arguments.isEmpty() || arguments.contains("tools")) {
      System.out.println();
      System.out.println("## Tools");
      System.out.println();
      var finder = bach.configuration().tooling().finder();
      list(finder).stream().map(describe::item).forEach(System.out::println);
    }
  }

  static List<ModuleReference> list(ModuleFinder finder) {
    return finder.findAll().stream()
        .sorted(Comparator.comparing(ref -> ref.descriptor().name()))
        .toList();
  }

  static List<ToolProvider> list(ToolFinder finder) {
    return finder.findAll().stream().sorted(Comparator.comparing(ToolProvider::name)).toList();
  }

  static String item(ModuleReference reference) {
    var descriptor = reference.descriptor();
    var name = descriptor.toNameAndVersion();
    var location = reference.location().map(Object::toString).orElse("?");
    return String.format("- `%s` in <%s>", name, location);
  }

  static String item(ToolProvider provider) {
    var name = provider.name();
    var module = provider.getClass().getModule();
    var source = module.isNamed() ? module.getDescriptor().toNameAndVersion() : module;
    var service = provider.getClass().getCanonicalName();
    return String.format("- `%s` by `%s/%s`", name, source, service);
  }
}
