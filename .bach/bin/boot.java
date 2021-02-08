package bin;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class boot {

  public static Bach bach() {
    return bach.get();
  }

  public static void beep() {
    System.out.print("\007"); // 🔔
    System.out.flush();
  }

  public static void refresh() {
    utils.refresh("configuration");
  }

  public interface configuration {
    static void init() throws Exception {
      init("configuration", "Modulation");
    }

    static void init(String moduleName, String providerName) throws Exception {
      var moduleDirectory = bach().base().directory(".bach", moduleName);
      if (Files.exists(moduleDirectory)) {
        out("Configuration module directory already exists: %s", moduleDirectory);
        return;
      }
      Files.createDirectories(moduleDirectory);

      Files.writeString(
          moduleDirectory.resolve("module-info.java"),
          """
          // @com.github.sormuras.bach.ProjectInfo()
          module {{MODULE-NAME}} {
            requires com.github.sormuras.bach;
            provides com.github.sormuras.bach.Bach with configuration.{{PROVIDER-NAME}};
          }
          """
              .replace("{{MODULE-NAME}}", moduleName)
              .replace("{{PROVIDER-NAME}}", providerName));

      Files.writeString(
          moduleDirectory.resolve(providerName + ".java"),
          """
          package configuration;

          import com.github.sormuras.bach.*;
          import com.github.sormuras.bach.lookup.*;

          public class {{PROVIDER-NAME}} extends Bach {
            public {{PROVIDER-NAME}}() {}

            @Override
            protected Finder newFinder() throws Exception {
              return Finder.empty()
                  // .with(Finders.JUnit.V_5_7_1)
                  // .with(new GitHubReleasesModuleLookup(this))
                  // .with(new ToolProvidersModuleLookup(this, Bach.EXTERNALS))
                  ;
            }

            @Override
            protected Project newProject() throws Exception {
              return super.newProject().version("1-ea");
            }
          }
          """
              .replace("{{PROVIDER-NAME}}", providerName));
    }
  }

  public interface files {

    static void dir() {
      dir("");
    }

    static void dir(String folder) {
      dir(folder, "*");
    }

    static void dir(String folder, String glob) {
      var win = System.getProperty("os.name", "?").toLowerCase(Locale.ROOT).contains("win");
      var directory = Path.of(folder).toAbsolutePath().normalize();
      var paths = new ArrayList<Path>();
      try (var stream = Files.newDirectoryStream(directory, glob)) {
        for (var path : stream) {
          if (win && Files.isHidden(path)) continue;
          paths.add(path);
        }
      } catch (Exception exception) {
        out(exception);
      }
      paths.sort(
          (Path p1, Path p2) -> {
            var one = Files.isDirectory(p1);
            var two = Files.isDirectory(p2);
            if (one && !two) return -1; // directory before file
            if (!one && two) return 1; // file after directory
            return p1.compareTo(p2); // order lexicographically
          });
      long files = 0;
      long bytes = 0;
      for (var path : paths) {
        var name = path.getFileName().toString();
        if (Files.isDirectory(path)) out("%-15s %s", "[+]", name);
        else
          try {
            files++;
            var size = Files.size(path);
            bytes += size;
            out("%,15d %s", size, name);
          } catch (Exception exception) {
            out(exception);
            return;
          }
      }
      var all = paths.size();
      if (all == 0) {
        out("Directory %s is empty", directory);
        return;
      }
      out("");
      out("%15d path%s in directory %s", all, all == 1 ? "" : "s", directory);
      out("%,15d bytes in %d file%s", bytes, files, files == 1 ? "" : "s");
    }

    static void tree() {
      tree("");
    }

    static void tree(String folder) {
      tree(folder, name -> name.contains("module-info"));
    }

    static void tree(String folder, Predicate<String> fileNameFilter) {
      var directory = Path.of(folder).toAbsolutePath();
      out("%s", folder.isEmpty() ? directory : folder);
      var files = tree(directory, "  ", fileNameFilter);
      out("");
      out("%d file%s in tree of %s", files, files == 1 ? "" : "s", directory);
    }

    private static int tree(Path directory, String indent, Predicate<String> filter) {
      var win = System.getProperty("os.name", "?").toLowerCase(Locale.ROOT).contains("win");
      var files = 0;
      try (var stream = Files.newDirectoryStream(directory, "*")) {
        for (var path : stream) {
          if (win && Files.isHidden(path)) continue;
          var name = path.getFileName().toString();
          if (Files.isDirectory(path)) {
            out(indent + name + "/");
            if (name.equals(".git")) continue;
            files += tree(path, indent + "  ", filter);
            continue;
          }
          files++;
          if (filter.test(name)) out(indent + name);
        }
      } catch (Exception exception) {
        out(exception);
      }
      return files;
    }
  }

  public interface modules {

    /**
     * Prints a module description of the given module.
     *
     * @param module the name of the module to describe
     */
    static void describe(String module) {
      ModuleFinder.compose(
              ModuleFinder.of(bach().base().workspace("modules")),
              ModuleFinder.of(bach().base().externals()),
              ModuleFinder.ofSystem())
          .find(module)
          .ifPresentOrElse(
              reference -> out.accept(describe(reference)),
              () -> out.accept("No such module found: " + module));
    }

    /**
     * Print a sorted list of all modules locatable by the given module finder.
     *
     * @param finder the module finder to query for modules
     */
    static void describe(ModuleFinder finder) {
      var all = finder.findAll();
      all.stream()
          .map(ModuleReference::descriptor)
          .map(ModuleDescriptor::toNameAndVersion)
          .sorted()
          .forEach(out);
      out("%n-> %d module%s", all.size(), all.size() == 1 ? "" : "s");
    }

    // https://github.com/openjdk/jdk/blob/80380d51d279852f4a24ebbd384921106611bc0c/src/java.base/share/classes/sun/launcher/LauncherHelper.java#L1105
    static String describe(ModuleReference mref) {
      var md = mref.descriptor();
      var writer = new StringWriter();
      var print = new PrintWriter(writer);

      // one-line summary
      print.print(md.toNameAndVersion());
      mref.location().filter(uri -> !isJrt(uri)).ifPresent(uri -> print.format(" %s", uri));
      if (md.isOpen()) print.print(" open");
      if (md.isAutomatic()) print.print(" automatic");
      print.println();

      // unqualified exports (sorted by package)
      md.exports().stream()
          .filter(e -> !e.isQualified())
          .sorted(Comparator.comparing(ModuleDescriptor.Exports::source))
          .forEach(e -> print.format("exports %s%n", toString(e.source(), e.modifiers())));

      // dependences (sorted by name)
      md.requires().stream()
          .sorted(Comparator.comparing(ModuleDescriptor.Requires::name))
          .forEach(r -> print.format("requires %s%n", toString(r.name(), r.modifiers())));

      // service use and provides (sorted by name)
      md.uses().stream().sorted().forEach(s -> print.format("uses %s%n", s));
      md.provides().stream()
          .sorted(Comparator.comparing(ModuleDescriptor.Provides::service))
          .forEach(
              ps -> {
                var names = String.join("\n", new TreeSet<>(ps.providers()));
                print.format("provides %s with%n%s", ps.service(), names.indent(2));
              });

      // qualified exports (sorted by package)
      md.exports().stream()
          .filter(ModuleDescriptor.Exports::isQualified)
          .sorted(Comparator.comparing(ModuleDescriptor.Exports::source))
          .forEach(
              e -> {
                var who = String.join("\n", new TreeSet<>(e.targets()));
                print.format("qualified exports %s to%n%s", e.source(), who.indent(2));
              });

      // open packages (sorted by package)
      md.opens().stream()
          .sorted(Comparator.comparing(ModuleDescriptor.Opens::source))
          .forEach(
              opens -> {
                if (opens.isQualified()) print.print("qualified ");
                print.format("opens %s", toString(opens.source(), opens.modifiers()));
                if (opens.isQualified()) {
                  var who = String.join("\n", new TreeSet<>(opens.targets()));
                  print.format(" to%n%s", who.indent(2));
                } else print.println();
              });

      // non-exported/non-open packages (sorted by name)
      var concealed = new TreeSet<>(md.packages());
      md.exports().stream().map(ModuleDescriptor.Exports::source).forEach(concealed::remove);
      md.opens().stream().map(ModuleDescriptor.Opens::source).forEach(concealed::remove);
      concealed.forEach(p -> print.format("contains %s%n", p));

      return writer.toString().stripTrailing();
    }

    private static <T> String toString(String name, Set<T> modifiers) {
      var strings = modifiers.stream().map(e -> e.toString().toLowerCase());
      return Stream.concat(Stream.of(name), strings).collect(Collectors.joining(" "));
    }

    private static boolean isJrt(URI uri) {
      return (uri != null && uri.getScheme().equalsIgnoreCase("jrt"));
    }

    private static void findRequiresDirectivesMatching(ModuleFinder finder, String regex) {
      var descriptors =
          finder.findAll().stream()
              .map(ModuleReference::descriptor)
              .sorted(Comparator.comparing(ModuleDescriptor::name))
              .toList();
      for (var descriptor : descriptors) {
        var matched =
            descriptor.requires().stream()
                .filter(requires -> requires.name().matches(regex))
                .toList();
        if (matched.isEmpty()) continue;
        out.accept(descriptor.toNameAndVersion());
        matched.forEach(requires -> out.accept("  requires " + requires));
      }
    }

    interface external {

      static void delete(String module) throws Exception {
        var jar = bach().computeExternalModuleFile(module);
        out("Delete %s", jar);
        Files.deleteIfExists(jar);
      }

      static void purge() throws Exception {
        var externals = bach().base().externals();
        if (!Files.isDirectory(externals)) return;
        try (var jars = Files.newDirectoryStream(externals, "*.jar")) {
          for (var jar : jars)
            try {
              out("Delete %s", jar);
              Files.deleteIfExists(jar);
            } catch (Exception exception) {
              out("Delete failed: %s", jar);
            }
        }
      }

      /** Prints a list of all external modules. */
      static void list() {
        describe(ModuleFinder.of(bach().base().externals()));
      }

      static void load(String module) {
        bach().loadExternalModules(module);
        var set = bach().computeMissingExternalModules();
        if (set.isEmpty()) return;
        out("");
        missing.list(set);
      }

      static void findRequires(String regex) {
        var finder = ModuleFinder.of(bach().base().externals());
        findRequiresDirectivesMatching(finder, regex);
      }

      interface missing {

        static void list() {
          list(bach().computeMissingExternalModules());
        }

        private static void list(Set<String> modules) {
          var size = modules.size();
          modules.stream().sorted().forEach(out);
          out("%n-> %d module%s missing", size, size == 1 ? " is" : "s are");
        }

        static void resolve() {
          bach().loadMissingExternalModules();
        }
      }

      interface prepared {
        static void loadComGithubSormurasModules() {
          loadComGithubSormurasModules("0-ea");
        }

        static void loadComGithubSormurasModules(String version) {
          var module = "com.github.sormuras.modules";
          var jar = module + "@" + version + ".jar";
          var uri = "https://github.com/sormuras/modules/releases/download/" + version + "/" + jar;
          bach().browser().load(uri, bach().computeExternalModuleFile(module));
        }
      }
    }

    interface system {

      /** Prints a list of all system modules. */
      static void list() {
        describe(ModuleFinder.ofSystem());
      }

      static void findRequires(String regex) {
        findRequiresDirectivesMatching(ModuleFinder.ofSystem(), regex);
      }
    }
  }

  public interface tools {

    static String describe(ToolProvider provider) {
      var name = provider.name();
      var module = provider.getClass().getModule();
      var by =
          Optional.ofNullable(module.getDescriptor())
              .map(ModuleDescriptor::toNameAndVersion)
              .orElse(module.toString());
      var info =
          switch (name) {
            case "jar" -> "Create an archive for classes and resources, and update or restore them";
            case "javac" -> "Read Java class and interface definitions and compile them into"
                + " classes";
            case "javadoc" -> "Generate HTML pages of API documentation from Java source files";
            case "javap" -> "Disassemble one or more class files";
            case "jdeps" -> "Launch the Java class dependency analyzer";
            case "jlink" -> "Assemble and optimize a set of modules into a custom runtime image";
            case "jmod" -> "Create JMOD files and list the content of existing JMOD files";
            case "jpackage" -> "Package a self-contained Java application";
            case "junit" -> "Launch the JUnit Platform";
            default -> provider.toString();
          };
      return "%s (provided by module %s)\n%s".formatted(name, by, info.indent(2)).trim();
    }

    static void list() {
      var providers = bach().computeToolProviders().toList();
      var size = providers.size();
      providers.stream()
          .map(tools::describe)
          .sorted()
          .map(description -> "\n" + description)
          .forEach(out);
      out("%n-> %d tool%s", size, size == 1 ? "" : "s");
    }

    static void recordings() {
      var list = bach().recordings();
      var size = list.size();
      list.forEach(out);
      out("%n-> %d recording%s", size, size == 1 ? "" : "s");
    }

    static void run(String tool, Object... args) {
      var command = Command.of(tool);
      var recording = bach().run(args.length == 0 ? command : command.add("", args));
      if (!recording.errors().isEmpty()) out.accept(recording.errors());
      if (!recording.output().isEmpty()) out.accept(recording.output());
      if (recording.isError())
        out.accept("Tool " + tool + " returned exit code " + recording.code());
    }
  }

  public interface utils {
    private static void describeClass(Class<?> type) {
      Stream.of(type.getDeclaredMethods())
          .filter(utils::describeOnlyInterestingMethods)
          .sorted(Comparator.comparing(Method::getName).thenComparing(Method::getParameterCount))
          .map(utils::describeMethod)
          .forEach(out);
      list(type);
    }

    private static boolean describeOnlyInterestingClasses(Class<?> type) {
      if (utils.class.equals(type)) return false;
      var modifiers = type.getModifiers();
      return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers);
    }

    private static boolean describeOnlyInterestingMethods(Method method) {
      var modifiers = method.getModifiers();
      return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers);
    }

    private static String describeMethod(Method method) {
      var generic = method.toGenericString();
      var line = generic.replace('$', '.');
      var head = line.indexOf("bach.boot.");
      if (head > 0) line = line.substring(head + 10);
      var tail = line.indexOf(") throws");
      if (tail > 0) line = line.substring(0, tail + 1);
      if (!line.endsWith("()")) {
        line = line.replace("com.github.sormuras.bach.", "");
        line = line.replace("java.util.function.", "");
        line = line.replace("java.util.spi.", "");
        line = line.replace("java.util.", "");
        line = line.replace("java.lang.module.", "");
        line = line.replace("java.lang.", "");
      }
      if (line.isEmpty()) throw new RuntimeException("Empty description line for: " + generic);
      return line;
    }

    static void api() {
      list(boot.class);
    }

    private static void list(Class<?> current) {
      Stream.of(current.getDeclaredClasses())
          .filter(utils::describeOnlyInterestingClasses)
          .sorted(Comparator.comparing(Class::getName))
          .peek(declared -> out(""))
          .forEach(utils::describeClass);
    }

    static void refresh(String module) {
      try {
        set(Bach.of(module));
      } catch (Exception exception) {
        out(
            """

            Refresh failed: %s

              Falling back to default Bach instance.
            """,
            exception.getMessage());
        set(new Bach());
      }
    }

    private static void set(Bach instance) {
      bach.set(instance);
    }
  }

  private static final Consumer<Object> out = System.out::println;
  private static final AtomicReference<Bach> bach = new AtomicReference<>();

  static {
    refresh();
  }

  private static void out(Exception exception) {
    out("""
        #
        # %s
        #
        """, exception);
  }

  private static void out(String format, Object... args) {
    out.accept(args == null || args.length == 0 ? format : String.format(format, args));
  }

  /** Hidden default constructor. */
  private boot() {}
}
