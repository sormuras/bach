package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Recording;
import com.github.sormuras.bach.internal.ModuleLayerBuilder;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleFinder;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/** Methods related to finding and running provided tools. */
public interface ToolProviderAPI {

  Bach bach();

  default Stream<ToolProvider> computeToolProviders() {
    return computeToolProviders(ModuleFinder.of(bach().folders().externalModules()));
  }

  default Stream<ToolProvider> computeToolProviders(ModuleFinder finder) {
    var layer = new ModuleLayerBuilder().before(finder).build();
    return ServiceLoader.load(layer, ToolProvider.class).stream().map(ServiceLoader.Provider::get);
  }

  default ToolProvider computeToolProvider(String name) {
    var provider = computeToolProviders().filter(it -> it.name().equals(name)).findFirst();
    if (provider.isPresent()) return provider.get();
    throw new RuntimeException("No tool provider found for name: " + name);
  }

  default Recording run(Command<?> command) {
    bach().debug("Run %s", command.toLine());
    var provider = command instanceof ToolProvider it ? it : computeToolProvider(command.name());
    var arguments = command.toStrings();
    return run(provider, arguments);
  }

  default List<Recording> run(Command<?> command, Command<?>... commands) {
    return run(Stream.concat(Stream.of(command), Stream.of(commands)).toList());
  }

  default List<Recording> run(List<Command<?>> commands) {
    var size = commands.size();
    bach().debug("Run %d command%s", size, size == 1 ? "" : "s");
    if (size == 0) return List.of();
    if (size == 1) return List.of(run(commands.iterator().next()));
    var sequential = bach().is(Options.Flag.RUN_COMMANDS_SEQUENTIALLY);
    var stream = sequential ? commands.stream() : commands.stream().parallel();
    return stream.map(this::run).toList();
  }

  default Recording run(ModuleFinder finder, Command<?> command) {
    bach().debug("Run %s", command.toLine());
    var provider = computeToolProviders(finder).filter(it -> it.name().equals(command.name())).findFirst().orElseThrow();
    var arguments = command.toStrings();
    return run(provider, arguments);
  }

  default Recording run(ToolProvider provider, List<String> arguments) {
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
      var skips = bach().options().values(Options.Property.SKIP_TOOL);
      var skip = skips.contains(name);
      if (skip) bach().debug("Skip run of '%s' due to --skip-tool=%s", name, skips);
      code = skip ? 0 : provider.run(new PrintWriter(out), new PrintWriter(err), args);
    } finally {
      currentThread.setContextClassLoader(currentLoader);
    }

    var duration = Duration.between(start, Instant.now());
    var tid = currentThread.getId();
    var output = out.toString().trim();
    var errors = err.toString().trim();
    var recording = new Recording(name, arguments, tid, duration, code, output, errors);

    bach().record(recording);
    return recording;
  }
}
