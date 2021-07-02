package com.github.sormuras.bach.workflow;

import com.github.sormuras.bach.Bach;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

public record Runner(Bach bach) {

  public Optional<ToolProvider> findToolProvider(String name) {
    return streamToolProviders().filter(provider -> provider.name().equals(name)).findFirst();
  }

  public Stream<ToolProvider> streamToolProviders() {
    return streamToolProviders(ModuleFinder.of(bach().folders().externalModules()));
  }

  public Stream<ToolProvider> streamToolProviders(ModuleFinder finder, String... roots) {
    return streamToolProviders(finder, ModuleFinder.ofSystem(), false, roots);
  }

  public Stream<ToolProvider> streamToolProviders(
      ModuleFinder beforeFinder, ModuleFinder afterFinder, boolean assertions, String... roots) {
    var configuration =
        Configuration.resolveAndBind(
            beforeFinder, List.of(ModuleLayer.boot().configuration()), afterFinder, Set.of(roots));
    var controller =
        ModuleLayer.defineModulesWithOneLoader(
            configuration, List.of(ModuleLayer.boot()), ClassLoader.getPlatformClassLoader());
    var layer = controller.layer();
    if (assertions) for (var root : roots) layer.findLoader(root).setDefaultAssertionStatus(true);
    return ServiceLoader.load(layer, ToolProvider.class).stream().map(ServiceLoader.Provider::get);
  }

  public Logbook.Run run(ToolProvider provider, List<String> arguments) {
    var name = provider.name();
    var currentThread = Thread.currentThread();
    var currentLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(provider.getClass().getClassLoader());

    var out = new StringWriter();
    var err = new StringWriter();
    var args = arguments.toArray(String[]::new);
    var start = Instant.now();
    int code;
    try {
      code = provider.run(new PrintWriter(out), new PrintWriter(err), args);
    } finally {
      currentThread.setContextClassLoader(currentLoader);
    }

    var duration = Duration.between(start, Instant.now());
    var tid = currentThread.getId();
    var output = out.toString().trim();
    var errors = err.toString().trim();
    return new Logbook.Run(name, arguments, tid, duration, code, output, errors);
  }
}
