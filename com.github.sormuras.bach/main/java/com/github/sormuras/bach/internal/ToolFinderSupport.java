package com.github.sormuras.bach.internal;

import com.github.sormuras.bach.ToolFinder;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;

public sealed interface ToolFinderSupport permits ConstantInterface {
  record ToolProviderToolFinder(List<ToolProvider> findAll) implements ToolFinder {}

  record CompositeToolFinder(List<ToolFinder> finders) implements ToolFinder {
    @Override
    public List<ToolProvider> findAll() {
      return finders.stream().flatMap(finder -> finder.findAll().stream()).toList();
    }

    @Override
    public Optional<ToolProvider> find(String name) {
      for (var finder : finders) {
        var tool = finder.find(name);
        if (tool.isPresent()) return tool;
      }
      return Optional.empty();
    }
  }

  record ModuleLayerToolFinder(ModuleLayer layer, ServiceLoader<ToolProvider> loader) implements ToolFinder {
    @Override
    public List<ToolProvider> findAll() {
      synchronized (loader) {
        return loader.stream()
            .filter(service -> service.type().getModule().getLayer() == layer)
            .map(ServiceLoader.Provider::get)
            .toList();
      }
    }
  }

  record ServiceLoaderToolFinder(ServiceLoader<ToolProvider> loader) implements ToolFinder {
    @Override
    public List<ToolProvider> findAll() {
      synchronized (loader) {
        return loader.stream().map(ServiceLoader.Provider::get).toList();
      }
    }
  }

  record ProgramToolFinder(Path path, Path java, String argsfile) implements ToolFinder {

    @Override
    public List<ToolProvider> findAll() {
      var name = path.normalize().toAbsolutePath().getFileName().toString();
      return find(name).map(List::of).orElseGet(List::of);
    }

    @Override
    public Optional<ToolProvider> find(String name) {
      var directory = path.normalize().toAbsolutePath();
      if (!Files.isDirectory(directory)) return Optional.empty();
      if (!name.equals(directory.getFileName().toString())) return Optional.empty();
      var command = new ArrayList<String>();
      command.add(java.toString());
      var args = directory.resolve(argsfile);
      if (Files.isRegularFile(args)) {
        command.add("@" + args);
        return Optional.of(new ExecuteProgramToolProvider(name, command));
      }
      var jars = PathSupport.list(directory, PathSupport::isJarFile);
      if (jars.size() == 1) {
        command.add("-jar");
        command.add(jars.get(0).toString());
        return Optional.of(new ExecuteProgramToolProvider(name, command));
      }
      var javas = PathSupport.list(directory, PathSupport::isJavaFile);
      if (javas.size() == 1) {
        command.add(javas.get(0).toString());
        return Optional.of(new ExecuteProgramToolProvider(name, command));
      }
      throw new UnsupportedOperationException("Unknown program layout in " + directory.toUri());
    }
  }

  record ProgramsToolFinder(Path path, Path java, String argsfile) implements ToolFinder {
    @Override
    public List<ToolProvider> findAll() {
      return PathSupport.list(path, Files::isDirectory).stream()
          .map(directory -> new ProgramToolFinder(directory, java, argsfile))
          .map(ToolFinder::findAll)
          .flatMap(List::stream)
          .toList();
    }
  }

  record LayersToolFinder(Path path) implements ToolFinder {
    @Override
    public List<ToolProvider> findAll() {
      return PathSupport.list(path, Files::isDirectory).stream()
          .map(ModuleFinder::of)
          .map(moduleFinder -> ToolFinder.of(moduleFinder, false))
          .flatMap(toolFinder -> toolFinder.findAll().stream())
          .toList();
    }
  }
}
