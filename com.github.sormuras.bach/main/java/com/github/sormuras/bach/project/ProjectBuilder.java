package com.github.sormuras.bach.project;

import com.github.sormuras.bach.ProjectInfo;
import com.github.sormuras.bach.internal.Modules;
import com.github.sormuras.bach.internal.Paths;
import com.github.sormuras.bach.module.ModuleLink;
import com.github.sormuras.bach.module.ModuleSearcher;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
    var release = release(info.main().release());
    var declarations = declarations(release);

    var spaces =
        new CodeSpaces(
            new MainCodeSpace(
                declarations.mainModuleDeclarations(),
                newModulePaths(info.main().modulePaths()),
                release,
                jarslug(version),
                info.main().generateApiDocumentation(),
                info.main().generateCustomRuntimeImage(),
                newTweaks(info.main().tweaks())),
            new TestCodeSpace(
                declarations.testModuleDeclarations(),
                newModulePaths(info.test().modulePaths()),
                newTweaks(info.test().tweaks())),
            new TestPreviewCodeSpace(
                declarations.testPreviewModuleDeclarations(),
                newModulePaths(info.test().modulePaths()),
                newTweaks(info.test().tweaks())));

    var library = library(spaces.finder());

    return new Project(name(name), version, library, spaces);
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
      ModuleDeclarations mainModuleDeclarations,
      ModuleDeclarations testModuleDeclarations,
      ModuleDeclarations testPreviewModuleDeclarations) {}

  Declarations declarations(int release) {
    var paths = new ArrayList<>(Paths.findModuleInfoJavaFiles(Path.of(""), 9));

    var views = new TreeMap<String, ModuleDeclaration>();
    var tests = new TreeMap<String, ModuleDeclaration>();
    var mains = new TreeMap<String, ModuleDeclaration>();

    var isTestPreviewModule = isModuleOf("test-preview", "*");
    var isTestModule = isModuleOf("test", info.test().modules());
    var isMainModule = isModuleOf("main", info.main().modules());

    var feature = Runtime.version().feature();
    var iterator = paths.listIterator();
    while (iterator.hasNext()) {
      var path = iterator.next();
      if (path.startsWith(".bach")) continue;
      if (isTestPreviewModule.test(path)) {
        var declaration = ModuleDeclaration.of(path, feature);
        views.put(declaration.name(), declaration);
        iterator.remove();
        continue;
      }
      if (isTestModule.test(path)) {
        var declaration = ModuleDeclaration.of(path, feature);
        tests.put(declaration.name(), declaration);
        iterator.remove();
        continue;
      }
      if (isMainModule.test(path)) {
        var declaration = ModuleDeclaration.of(path, release);
        mains.put(declaration.name(), declaration);
        iterator.remove();
      }
    }
    return new Declarations(
        new ModuleDeclarations(mains),
        new ModuleDeclarations(tests),
        new ModuleDeclarations(views));
  }

  Library library(ModuleFinder finder) {
    var requires = new TreeSet<>(List.of(info.library().requires()));
    requires.addAll(Modules.required(finder));
    requires.removeAll(Modules.declared(ModuleFinder.ofSystem()));
    requires.removeAll(Modules.declared(finder));
    var links = new TreeMap<String, String>();
    for (var link : info.library().links()) links.put(link.module(), link(link).uri());
    var searchers = new ArrayList<ModuleSearcher>();
    for (var searcher : info.library().searchers()) searchers.add(searcher(searcher));
    return new Library(requires, links, searchers);
  }

  ModuleLink link(ProjectInfo.Library.Link link) {
    var module = link.module();
    var target = link.target();

    return switch (link.type()) {
      case AUTO -> ModuleLink.link(module).to(target);
      case URI -> ModuleLink.link(module).toUri(target);
      case MAVEN -> ModuleLink.link(module).toMaven(link.mavenRepository(), target);
    };
  }

  ModuleSearcher searcher(ProjectInfo.Library.Searcher searcher) {
    try {
      try {
        return searcher.with().getConstructor(String.class).newInstance(searcher.version());
      } catch (NoSuchMethodException exception) {
        return searcher.with().getConstructor().newInstance();
      }
    } catch (ReflectiveOperationException exception) {
      throw new RuntimeException("Creating module searcher failed: " + searcher.with(), exception);
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
