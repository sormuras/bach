package com.github.sormuras.bach.project;

import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.internal.Paths;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Creates project instances. */
class ProjectBuilder {

  private final String name;
  private final ProjectInfo info;

  ProjectBuilder(String name, ProjectInfo info) {
    this.name = name;
    this.info = info;
  }

  Project build() {
    var version = version(info.version());
    var features = newFeatures(info.features());
    var declarations = newDeclarations(features);

    var spaces =
        new CodeSpaces(
            new MainCodeSpace(
                declarations.mainModuleDeclarations(),
                newModulePaths(info.modulePaths()),
                release(info.compileModulesForJavaRelease()),
                jarslug(version),
                features,
                newTweaks(info.tweaks())),
            new TestCodeSpace(
                declarations.testModuleDeclarations(),
                newModulePaths(info.test().modulePaths()),
                newTweaks(info.test().tweaks())));

    var externalModules = newExternalModules(spaces.finder());

    return new Project(name(name), version, externalModules, spaces);
  }

  String name(String name) {
    return System.getProperty("bach.project.name", name);
  }

  Version version(String version) {
    return Version.parse(System.getProperty("bach.project.version", version));
  }

  int release(int release) {
    try {
      return Integer.parseInt(System.getProperty("bach.project.main.release"));
    } catch (RuntimeException ignore) {
      return release != 0 ? release : Runtime.version().feature();
    }
  }

  String jarslug(Version version) {
    return System.getProperty("bach.project.main.jarslug", version.toString());
  }

  private record Declarations(
      ModuleDeclarations mainModuleDeclarations, ModuleDeclarations testModuleDeclarations) {}

  Features newFeatures(Feature... features) {
    var property = System.getProperty("bach.project.main.features");
    if (property == null) return new Features(Set.of(features));
    var set = EnumSet.noneOf(Feature.class);
    for (var feature : property.split(",")) set.add(Feature.valueOf(feature));
    return new Features(set);
  }

  Declarations newDeclarations(Features features) {
    var paths = new ArrayList<>(Paths.findModuleInfoJavaFiles(Path.of(""), 9));

    var tests = new TreeMap<String, ModuleDeclaration>();
    var mains = new TreeMap<String, ModuleDeclaration>();

    var isTestModule = isModuleOf("test", info.test().modules());
    var isMainModule = isModuleOf("main", info.modules());
    var withSources = features.set().contains(Feature.INCLUDE_SOURCES_IN_MODULAR_JAR);

    var iterator = paths.listIterator();
    while (iterator.hasNext()) {
      var path = iterator.next();
      if (path.startsWith(".bach")) continue;
      if (isTestModule.test(path)) {
        var declaration = ModuleDeclaration.of(path, false);
        tests.put(declaration.name(), declaration);
        iterator.remove();
        continue;
      }
      if (isMainModule.test(path)) {
        var declaration = ModuleDeclaration.of(path, withSources);
        mains.put(declaration.name(), declaration);
        iterator.remove();
      }
    }
    return new Declarations(new ModuleDeclarations(mains), new ModuleDeclarations(tests));
  }

  ExternalModules newExternalModules(ModuleFinder finder) {
    var requires = new TreeSet<>(List.of(info.externalModules().requires()));
    requires.addAll(Modules.required(finder));
    requires.removeAll(Modules.declared(ModuleFinder.ofSystem()));
    requires.removeAll(Modules.declared(finder));
    var links = new TreeMap<String, ExternalModule>();
    for (var link : info.externalModules().links()) links.put(link.module(), link(link));
    var lookups = new ArrayList<ModuleLookup>();
    for (var lookup : info.externalModules().lookups()) lookups.add(newModuleLookup(lookup));
    return new ExternalModules(requires, links, lookups);
  }

  ExternalModule link(ProjectInfo.ExternalModules.Link link) {
    var module = link.module();
    var target = link.to();

    return switch (link.type()) {
      case AUTO -> ExternalModule.link(module).to(target);
      case URI -> ExternalModule.link(module).toUri(target);
      case MAVEN -> ExternalModule.link(module).toMaven(link.mavenRepository(), target);
    };
  }

  ModuleLookup newModuleLookup(Class<? extends ModuleLookup> lookup) {
    try {
      return lookup.getConstructor().newInstance();
    } catch (ReflectiveOperationException exception) {
      throw new RuntimeException("Creating module lookup failed: " + lookup, exception);
    }
  }

  ModulePaths newModulePaths(String... paths) {
    return new ModulePaths(Arrays.stream(paths).map(Path::of).collect(Collectors.toList()));
  }

  Tweaks newTweaks(ProjectInfo.Tweak... tweaks) {
    var map = new HashMap<String, Tweak>();
    for (var tweak : tweaks) map.put(tweak.tool(), new Tweak(tweak.tool(), List.of(tweak.args())));
    return new Tweaks(map);
  }

  static Predicate<Path> isModuleOf(String space, String... configuration) {
    if (configuration.length == 0) return path -> false;
    if (configuration.length == 1 && configuration[0].equals("*")) {
      if (space.equals("main")) return path -> true;
      else return path -> Paths.isModuleInfoJavaFileForCodeSpace(path, space);
    }
    return Set.of(configuration).stream().map(Path::of).collect(Collectors.toSet())::contains;
  }
}
