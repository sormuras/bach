import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

class describe {
  public static void main(String... args) {
    //    var bach = new Bach();
    //    var explorer = bach.explorer();
    //
    //    var arguments = List.of(args);
    //    if (arguments.isEmpty()) {
    //      System.out.println();
    //      System.out.printf("# Describe Bach %s%n", Bach.version());
    //      System.out.println();
    //      System.out.printf("  user.dir = `%s`%n", System.getProperty("user.dir"));
    //    }
    //
    //    if (arguments.isEmpty() || arguments.contains("modules")) {
    //      var externalModulesFinder = ModuleFinder.of(bach.path().externalModules());
    //      System.out.println();
    //      System.out.println("## Modules");
    //      System.out.println();
    //      System.out.println("### Declared Modules");
    //      System.out.println();
    //      System.out.println(describeModules(explorer.newModuleInfoFinder(bach.path().root())));
    //      System.out.println();
    //      System.out.println("### External Modules");
    //      System.out.println();
    //      System.out.println(describeModules(externalModulesFinder));
    //      System.out.println();
    //      System.out.println("### Missing External Modules");
    //      System.out.println();
    //      var names = explorer.listMissingExternalModules(externalModulesFinder);
    //      names.stream().map("- `%s`"::formatted).forEach(System.out::println);
    //      System.out.printf("  %d missing module%s%n", names.size(), names.size() == 1 ? "" :
    // "s");
    //      System.out.println();
    //      System.out.println("### System Modules");
    //      System.out.println();
    //      System.out.println(describeModules(ModuleFinder.ofSystem()));
    //    }
    //
    //    if (arguments.isEmpty() || arguments.contains("tools")) {
    //      System.out.println();
    //      System.out.println("## Tools");
    //      System.out.println();
    //      bach.printer().printTools();
    //    }
  }

  static String describeModules(ModuleFinder finder) {
    var modules = list(finder);
    var size = modules.size();
    var s = size == 1 ? "" : "s";
    var joiner = new StringJoiner("\n");
    modules.stream().map(describe::item).forEach(joiner::add);
    joiner.add(String.format("  %d module%s", size, s));
    return joiner.toString();
  }

  static List<ModuleReference> list(ModuleFinder finder) {
    return finder.findAll().stream()
        .sorted(Comparator.comparing(ref -> ref.descriptor().name()))
        .toList();
  }

  static String item(ModuleReference reference) {
    var descriptor = reference.descriptor();
    var name = descriptor.toNameAndVersion();
    var location = reference.location().map(Object::toString).orElse("?");
    return String.format("- `%s` in <%s>", name, location);
  }
}
