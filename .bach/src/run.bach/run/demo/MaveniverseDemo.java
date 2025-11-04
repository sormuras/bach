package run.demo;

import module java.base;
import module run.bach;

public class MaveniverseDemo {
  final List<ModuleLookup> lookups =
      List.of(
          ModuleLookup.ofJUnit("6.1.0-SNAPSHOT"),
          ModuleLookup.ofJUnit("6.0.1"),
          ModuleLookup.ofJUnit("5.14.1"),
          ModuleLookup.ofJUnit("6.0.0"),
          ModuleLookup.ofJUnit("5.14.0"),
          ModuleLookup.ofJUnit("5.14.0-RC1"),
          ModuleLookup.ofJUnit("6.0.0-RC3"),
          ModuleLookup.ofJUnit("6.0.0-RC2"),
          ModuleLookup.ofJUnit("6.0.0-RC1"),
          ModuleLookup.ofJUnit("6.0.0-M2"),
          ModuleLookup.ofJUnit("5.13.4"),
          ModuleLookup.ofJUnit("5.13.3"),
          ModuleLookup.ofJUnit("6.0.0-M1"),
          ModuleLookup.ofJUnit("5.13.2"),
          ModuleLookup.ofJUnit("5.13.1"),
          ModuleLookup.ofJUnit("5.13.0"),
          ModuleLookup.ofJUnit("5.13.0-RC1"),
          ModuleLookup.ofJUnit("5.13.0-M3"),
          ModuleLookup.ofJUnit("5.12.2"),
          ModuleLookup.ofJUnit("5.13.0-M2"),
          ModuleLookup.ofJUnit("5.13.0-M1"),
          ModuleLookup.ofJUnit("5.12.1"),
          ModuleLookup.ofJUnit("5.12.0"),
          ModuleLookup.ofJUnit("5.12.0-RC2"),
          ModuleLookup.ofJUnit("5.12.0-RC1"),
          ModuleLookup.ofJUnit("5.12.0-M1"),
          ModuleLookup.ofJUnit("5.11.4"),
          ModuleLookup.ofJUnit("5.11.3"),
          ModuleLookup.ofJUnit("5.11.2"),
          ModuleLookup.ofJUnit("5.10.5"),
          ModuleLookup.ofJUnit("5.11.1"),
          ModuleLookup.ofJUnit("5.10.4"),
          ModuleLookup.ofJUnit("5.11.0"),
          ModuleLookup.ofJUnit("5.11.0-RC1"),
          ModuleLookup.ofJUnit("5.10.3"),
          ModuleLookup.ofJUnit("5.11.0-M2"),
          ModuleLookup.ofJUnit("5.11.0-M1"),
          ModuleLookup.ofJUnit("5.10.2"),
          ModuleLookup.ofJUnit("5.10.1"),
          ModuleLookup.ofJUnit("5.10.0"),
          ModuleLookup.ofJUnit("5.10.0-RC2"),
          ModuleLookup.ofJUnit("5.10.0-RC1"),
          ModuleLookup.ofJUnit("5.10.0-M1"),
          ModuleLookup.ofJUnit("5.9.3"),
          ModuleLookup.ofJUnit("5.9.2"),
          ModuleLookup.ofJUnit("5.9.1"),
          ModuleLookup.ofJUnit("5.9.0"),
          ModuleLookup.ofJUnit("5.9.0-RC1"),
          ModuleLookup.ofJUnit("5.9.0-M1"),
          ModuleLookup.ofJUnit("5.8.2"),
          ModuleLookup.ofJUnit("5.8.1"),
          ModuleLookup.ofJUnit("5.8.0"), // not going before 5.8.0, as Suite was introduced here
          ModuleLookup.of("junit-start", "experiments~junit-onramp-SNAPSHOT")
              .withRepository("https://jitpack.io")
              .withBill("com.github.junit-team.junit-framework:junit-bom:{{version}}")
              .withRoot("com.github.junit-team.junit-framework:junit-start"));

  void main() throws Exception {
    var base = "https://repo.maven.apache.org/maven2/eu/maveniverse/maven/plugins/toolbox/";
    var toolbox = Tool.of(base + "0.14.3/toolbox-0.14.3-cli.jar");
    toolbox.run("--version");

    var runner = ToolRunner.ofSilence();
    for (var lookup : lookups) {
      var call = ToolCall.of(toolbox).add("--batch-mode").add("copy-transitive");
      if (!lookup.repositories().isEmpty()) {
        call = call.add("--extra-repositories", String.join(",", lookup.repositories()));
      }
      if (!lookup.materials().isEmpty()) {
        call = call.add("--boms", String.join(",", lookup.materials()));
      }
      call = call.add("stat()").add(String.join(",", lookup.roots()));
      IO.println("| " + call.toCommandLine());
      var entries =
          runner
              .run(call)
              .out()
              .lines()
              .filter(line -> line.contains("Module name:") || line.contains("Origin URI:"))
              .map(line -> line.substring(line.indexOf(':') + 1).strip())
              .gather(Gatherers.windowFixed(2))
              .map(MaveniverseDemo::computeProperty)
              .distinct()
              .sorted()
              .toList();
      var folder =
          Files.createDirectories(
                  Path.of(
                      ".bach",
                      "out",
                      "module-lookups",
                      lookup.version().contains("-") ? "early-access" : ""))
              .normalize();
      Files.write(folder.resolve(lookup.toFilename()), entries);
    }
  }

  private static String computeProperty(List<String> strings) {
    if (strings.size() != 2) throw new AssertionError("Expected 2 elements, but got: " + strings);
    var moduleName = computeModuleName(strings.getFirst());
    var originUri = computeUri(strings.getLast());
    return moduleName + "=" + originUri;
  }

  private static String computeModuleName(String name) {
    return ModuleDescriptor.newModule(name).build().name();
  }

  private static String computeUri(String string) {
    return URI.create(string).toASCIIString();
  }

  record ModuleLookup(
      String name,
      String version,
      List<String> repositories,
      List<String> materials,
      List<String> roots) {
    static ModuleLookup of(String name, String version) {
      return new ModuleLookup(name, version, List.of(), List.of(), List.of());
    }

    static ModuleLookup ofJUnit(String version) {
      var lookup = ModuleLookup.of("junit", version);
      if (version.contains("SNAPSHOT")) {
        lookup = lookup.withRepository("https://central.sonatype.com/repository/maven-snapshots");
      }
      lookup =
          lookup
              .withBill("org.junit:junit-bom:{{version}}")
              .withRoot("org.junit.jupiter:junit-jupiter")
              .withRoot("org.junit.platform:junit-platform-console")
              .withRoot("org.junit.platform:junit-platform-suite");
      return lookup;
    }

    ModuleLookup withRepository(String repository) {
      var repositories = Stream.concat(repositories().stream(), Stream.of(repository)).toList();
      return new ModuleLookup(name, version, repositories, materials, roots);
    }

    ModuleLookup withBill(String gav) {
      var material = gav.replace("{{version}}", version);
      var materials = Stream.concat(materials().stream(), Stream.of(material)).toList();
      return new ModuleLookup(name, version, repositories, materials, roots);
    }

    ModuleLookup withRoot(String gav) {
      var root = gav.replace("{{version}}", version);
      var roots = Stream.concat(roots().stream(), Stream.of(root)).toList();
      return new ModuleLookup(name, version, repositories, materials, roots);
    }

    String toFilename() {
      return name + "@" + version + ".properties";
    }
  }
}
