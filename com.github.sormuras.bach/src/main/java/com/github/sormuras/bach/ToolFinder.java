package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.ModuleLayerSupport;
import com.github.sormuras.bach.internal.PathSupport;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.BiPredicate;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/**
 * A finder of tools.
 *
 * <p>What {@link java.lang.module.ModuleFinder ModuleFinder} is to {@link
 * java.lang.module.ModuleReference ModuleReference}, is {@link ToolFinder} to {@link Tool}.
 */
@FunctionalInterface
public interface ToolFinder {

  List<Tool> findAll();

  default List<Tool> find(String name) {
    return find(name, Tool::isNameMatching);
  }

  default List<Tool> find(String name, BiPredicate<Tool, String> filter) {
    return findAll().stream().filter(tool -> filter.test(tool, name)).toList();
  }

  default List<ToolFinder> decompose() {
    return List.of(this);
  }

  static ToolFinder of(Tool... tools) {
    record DirectToolFinder(List<Tool> findAll) implements ToolFinder {}
    return new DirectToolFinder(List.of(tools));
  }

  static ToolFinder of(ClassLoader loader) {
    return ToolFinder.of(ServiceLoader.load(ToolProvider.class, loader));
  }

  static ToolFinder of(ServiceLoader<ToolProvider> loader) {
    record ServiceLoaderToolFinder(ServiceLoader<ToolProvider> loader) implements ToolFinder {
      @Override
      public List<Tool> findAll() {
        synchronized (loader) {
          return loader.stream().map(ServiceLoader.Provider::get).map(Tool::of).toList();
        }
      }
    }
    return new ServiceLoaderToolFinder(loader);
  }

  static ToolFinder of(ModuleFinder finder, boolean assertions, String... roots) {
    return of(ModuleLayerSupport.layer(finder, assertions, roots));
  }

  static ToolFinder of(ModuleLayer layer) {
    record ServiceLoaderToolFinder(ModuleLayer layer, ServiceLoader<ToolProvider> loader)
        implements ToolFinder {
      @Override
      public List<Tool> findAll() {
        synchronized (loader) {
          return loader.stream()
              .filter(service -> service.type().getModule().getLayer() == layer)
              .map(ServiceLoader.Provider::get)
              .map(Tool::of)
              .toList();
        }
      }
    }
    var loader = ServiceLoader.load(layer, ToolProvider.class);
    return new ServiceLoaderToolFinder(layer, loader);
  }

  static ToolFinder ofSystemTools() {
    return ToolFinder.of(ClassLoader.getSystemClassLoader());
  }

  static ToolFinder ofNativeToolsInJavaHome(String name, String... more) {
    var tools = new ArrayList<Tool>();
    tools.add(Tool.ofNativeToolInJavaHome(name));
    Stream.of(more).map(Tool::ofNativeToolInJavaHome).forEach(tools::add);
    return ToolFinder.of(tools.toArray(Tool[]::new));
  }

  static ToolFinder ofJavaTools(String directory) {
    return ofJavaTools(Path.of(directory));
  }

  static ToolFinder ofJavaTools(Path directory) {
    var java = Path.of(System.getProperty("java.home"), "bin", "java");
    return ofJavaTools(directory, java, "java.args");
  }

  static ToolFinder ofJavaTools(Path directory, Path java, String argsfile) {
    record ProgramToolFinder(Path path, Path java, String argsfile) implements ToolFinder {

      @Override
      public List<Tool> findAll() {
        return find(path.normalize().toAbsolutePath().getFileName().toString());
      }

      @Override
      public List<Tool> find(String name) {
        var directory = path.normalize().toAbsolutePath();
        if (!Files.isDirectory(directory)) return List.of();
        var namespace = path.getParent().getFileName().toString();
        if (!name.equals(directory.getFileName().toString())) return List.of();
        var command = new ArrayList<String>();
        command.add(java.toString());
        var args = directory.resolve(argsfile);
        if (Files.isRegularFile(args)) {
          command.add("@" + args);
          return List.of(Tool.ofNativeTool(namespace + '/' + name, command));
        }
        var jars = PathSupport.list(directory, PathSupport::isJarFile);
        if (jars.size() == 1) {
          command.add("-jar");
          command.add(jars.get(0).toString());
          return List.of(Tool.ofNativeTool(namespace + '/' + name, command));
        }
        var javas = PathSupport.list(directory, PathSupport::isJavaFile);
        if (javas.size() == 1) {
          command.add(javas.get(0).toString());
          return List.of(Tool.ofNativeTool(namespace + '/' + name, command));
        }
        return List.of();
      }
    }
    record ProgramsToolFinder(Path path, Path java, String argsfile) implements ToolFinder {
      @Override
      public List<Tool> findAll() {
        return PathSupport.list(path, Files::isDirectory).stream()
            .map(directory -> new ProgramToolFinder(directory, java, argsfile))
            .map(ToolFinder::findAll)
            .flatMap(List::stream)
            .toList();
      }
    }
    return new ProgramsToolFinder(directory, java, argsfile);
  }

  static ToolFinder compose(ToolFinder... finders) {
    return compose(List.of(finders));
  }

  static ToolFinder compose(List<ToolFinder> finders) {
    record CompositeToolFinder(List<ToolFinder> finders) implements ToolFinder {
      @Override
      public List<ToolFinder> decompose() {
        return finders;
      }

      @Override
      public List<Tool> findAll() {
        return finders.stream().flatMap(finder -> finder.findAll().stream()).toList();
      }

      @Override
      public List<Tool> find(String name) {
        return finders.stream().flatMap(finder -> finder.find(name).stream()).toList();
      }
    }
    return new CompositeToolFinder(finders);
  }
}
