package de.sormuras.bach;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiModuleCompiler {

  private final Domain.Project project;

  public MultiModuleCompiler(Domain.Project project) {
    this.project = project;
  }

  public List<Command> toCommands(Domain.Realm realm, Collection<String> modules) {
    var target = project.target.resolve("realm").resolve(realm.name);
    var destination = target.resolve("exploded").resolve("multi-module");
    var javac =
        new Command("javac")
            .add("-d", destination)
            .add("--module-source-path", realm.moduleSourcePath)
            .add("--module-version", project.version)
            .add("--module", String.join(",", modules));

    var commands = new ArrayList<>(List.of(javac));
    for (var module : modules) {
      var source = realm.modules.get(module);
      var version = source.descriptor.version();
      var jarFile = module + "-" + version.orElse(project.version);
      var jar = target.resolve("modules").resolve(jarFile + ".jar");
      var resources = source.resources;

      Util.treeCreate(jar.getParent()); // jar doesn't create directories...
      commands.add(
          new Command("jar")
              .add("--create")
              .add("--file", jar)
              // .addIff(configuration.debug(), "--verbose")
              .addIff("--module-version", version)
              .addIff("--main-class", source.descriptor.mainClass())
              .add("-C", destination.resolve(module))
              .add(".")
              .addIff(Files.isDirectory(resources), cmd -> cmd.add("-C", resources).add(".")));

      var sourcesJar = target.resolve("sources").resolve(jarFile + "-sources.jar");
      Util.treeCreate(sourcesJar.getParent()); // jar still doesn't create directories...
      commands.add(
          new Command("jar")
              .add("--create")
              .add("--file", sourcesJar)
              // .addIff(configuration.debug(), "--verbose")
              .add("--no-manifest")
              .add("-C", source.sources)
              .add(".")
              .addIff(Files.isDirectory(resources), cmd -> cmd.add("-C", resources).add(".")));
    }

    return List.copyOf(commands);
  }
}
