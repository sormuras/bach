package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.project.Flag;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.internal.ModuleLayerBuilder;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleFinder;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/** Methods related to finding and running provided tools. */
public interface ToolProviderAPI extends API {

  default Stream<ToolProvider> computeToolProviders() {
    return computeToolProviders(ModuleFinder.of(bach().folders().externalModules()));
  }

  default Stream<ToolProvider> computeToolProviders(ModuleFinder finder, String... roots) {
    var layer = new ModuleLayerBuilder().before(finder).roots(Set.of(roots)).build();
    return ServiceLoader.load(layer, ToolProvider.class).stream().map(ServiceLoader.Provider::get);
  }

  default Optional<ToolProvider> computeToolProvider(String name) {
    return computeToolProviders().filter(it -> it.name().equals(name)).findFirst();
  }

  default Logbook.Run run(Command<?> command) {
    return run(command, command.toDescription(117));
  }

  default Logbook.Run run(Command<?> command, String description) {
    var name = command.name();
    var arguments = command.arguments();
    if (command instanceof ToolProvider provider) return run(provider, arguments, description);
    var provider = computeToolProvider(name);
    if (provider.isEmpty()) throw new RuntimeException("No tool provider found for name: " + name);
    return run(provider.get(), arguments, description);
  }

  default Logbook.Runs run(Command<?> command, Command<?>... commands) {
    return run(Stream.concat(Stream.of(command), Stream.of(commands)));
  }

  default Logbook.Runs run(Stream<? extends Command<?>> commands) {
    var sequential = bach().is(Flag.RUN_COMMANDS_SEQUENTIALLY);
    log("Stream tool runs %s", sequential ? "sequentially" : "in parallel");
    var stream = sequential ? commands.sequential() : commands.parallel();
    var runs = stream.map(this::run).toList();
    log("Collected %d tool run%s", runs.size(), runs.size() == 1 ? "" : "s");
    return new Logbook.Runs(runs);
  }

  default Logbook.Run run(Command<?> command, ModuleFinder finder, String... roots) {
    var providers = computeToolProviders(finder, roots);
    var provider = providers.filter(it -> it.name().equals(command.name())).findFirst();
    return run(provider.orElseThrow(), command.arguments(), command.toDescription(117));
  }

  default Logbook.Run run(ToolProvider provider, List<String> arguments, String description) {
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
      var enabled = bach().project().settings().tools().enabled(name);
      if (enabled) say("  %-8s %s", name, description);
      else log("Skip %s", name);
      code = enabled ? provider.run(new PrintWriter(out), new PrintWriter(err), args) : 0;
    } finally {
      currentThread.setContextClassLoader(currentLoader);
    }

    var duration = Duration.between(start, Instant.now());
    var tid = currentThread.getId();
    var output = out.toString().trim();
    var errors = err.toString().trim();
    var recording = new Logbook.Run(name, arguments, tid, duration, code, output, errors);

    bach().logbook().log(recording);
    return recording;
  }
}
