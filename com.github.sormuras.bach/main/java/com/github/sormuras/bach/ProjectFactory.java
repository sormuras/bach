package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.module.ModuleInfoFinder;
import com.github.sormuras.bach.module.ModuleInfoReference;
import com.github.sormuras.bach.module.ModuleLink;
import com.github.sormuras.bach.project.Library;
import com.github.sormuras.bach.project.MainSpace;
import com.github.sormuras.bach.project.ModuleSupplement;
import com.github.sormuras.bach.project.TestSpace;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

class ProjectFactory {

  private final String name;
  private final ProjectInfo info;

  ProjectFactory(String name, ProjectInfo info) {
    this.name = name;
    this.info = info;
  }

  Project newProject() {
    var version = version(info.version());

    var main = info.main();
    var mainModuleInfoFinder = mainModuleInfoFinder(main.moduleSourcePaths());
    var test = info.test();
    var testModuleInfoFinder = ModuleInfoFinder.of(test.moduleSourcePaths());

    return new Project(
        name(name),
        version,
        library(ModuleFinder.compose(mainModuleInfoFinder, testModuleInfoFinder)),
        new MainSpace(
            modules(main.modules(), mainModuleInfoFinder),
            mainModuleInfoFinder.moduleSourcePaths(),
            List.of(main.modulePaths()),
            release(main.release()),
            supplements(mainModuleInfoFinder),
            jarslug(version),
            main.generateApiDocumentation(),
            main.generateCustomRuntimeImage(),
            main.generateApplicationPackage(),
            tweaks(main.tweaks())),
        new TestSpace(
            modules(test.modules(), testModuleInfoFinder),
            testModuleInfoFinder.moduleSourcePaths(),
            List.of(test.modulePaths()),
            tweaks(test.tweaks())));
  }

  String name(String name) {
    return System.getProperty("bach.project.name", name);
  }

  ModuleDescriptor.Version version(String version) {
    return ModuleDescriptor.Version.parse(System.getProperty("bach.project.version", version));
  }

  int release(int release) {
    try {
      return Integer.parseInt(System.getProperty("bach.project.main.release"));
    } catch (RuntimeException ignore) {
      return release != 0 ? release : Runtime.version().feature();
    }
  }

  String jarslug(ModuleDescriptor.Version version) {
    return System.getProperty("bach.project.main.jarslug", version.toString());
  }

  Map<String, ModuleSupplement> supplements(ModuleInfoFinder finder) {
    var map = new TreeMap<String, ModuleSupplement>();
    var currentWorkingDirectory = Path.of("").toAbsolutePath();
    for (var reference : finder.findAll()) {
      var info = Path.of(reference.location().orElseThrow());
      // "java-N", with N = 7, 8, ... 16
      var releases = new TreeSet<Integer>();
      var path = info.getParent().getParent();
      var paths = Paths.list(path, p -> Paths.name(p).matches("java-\\d+"));
      for (var pathN : paths) releases.add(Integer.parseInt(Paths.name(pathN).substring(5)));
      var descriptor = reference.descriptor();
      map.put(
          descriptor.name(),
          new ModuleSupplement(
              currentWorkingDirectory.relativize(info), descriptor, List.copyOf(releases)));
    }
    return Map.copyOf(map);
  }

  Library library(ModuleFinder finder) {
    var requires = new TreeSet<>(List.of(info.library().requires()));
    requires.addAll(Modules.required(finder));
    requires.removeAll(Modules.declared(ModuleFinder.ofSystem()));
    requires.removeAll(Modules.declared(finder));
    var links = new TreeMap<String, String>();
    var binding = ProjectInfo.Library.Binding.ofSystem();
    for (var link : info.library().links()) links.put(link.module(), link(link, binding).uri());
    return new Library(requires, links);
  }

  ModuleLink link(ProjectInfo.Library.Link link, Map<String, String> binding) {
    var module = link.module();
    var target = replace(link.target(), binding);

    return switch (link.type()) {
      case AUTO -> ModuleLink.link(module).to(target);
      case URI -> ModuleLink.link(module).toUri(target);
      case MAVEN -> ModuleLink.link(module).toMaven(link.mavenRepository(), target);
    };
  }

  String replace(String template, Map<String, String> binding) {
    if (template.indexOf('{') < 0) return template;
    var replaced = template;
    for (var entry : binding.entrySet()) {
      var placeholder = entry.getKey();
      var replacement = entry.getValue();
      replaced = replaced.replace(placeholder, replacement);
    }
    return replaced;
  }

  ModuleInfoFinder mainModuleInfoFinder(String[] moduleSourcePaths) {
    // try configured or default module source paths, usually patterns
    var finder = ModuleInfoFinder.of(moduleSourcePaths);
    // no module declaration found
    if (finder.findAll().isEmpty()) {
      // try single module declaration or what is "simplicissimus"?
      var info = Path.of("module-info.java");
      if (Files.exists(info)) return ModuleInfoFinder.of(ModuleInfoReference.of(info));
      // assume modules are declared in directories named like modules
      return ModuleInfoFinder.of(".");
    }
    return finder;
  }

  List<String> modules(String[] modules, ModuleFinder finder) {
    var all = modules.length == 1 && modules[0].equals("*");
    return all ? List.copyOf(Modules.declared(finder)) : List.of(modules);
  }

  Map<String, List<String>> tweaks(ProjectInfo.Tweak... tweaks) {
    var map = new HashMap<String, List<String>>();
    for (var tweak : tweaks) map.put(tweak.tool(), List.of(tweak.args()));
    return Map.copyOf(map);
  }
}
