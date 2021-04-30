package com.github.sormuras.bach.core;

import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.CommandResult;
import com.github.sormuras.bach.CommandResults;
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

public /*sealed*/ interface Commander extends BachTrait {

  int MAX_DESCRIPTION_LENGTH = 117;

  default Stream<ToolProvider> streamToolProviders() {
    return streamToolProviders(ModuleFinder.of(bach().project().folders().externals()));
  }

  default Stream<ToolProvider> streamToolProviders(ModuleFinder finder, String... roots) {
    return streamToolProviders(finder, ModuleFinder.ofSystem(), false, roots);
  }

  default Stream<ToolProvider> streamToolProviders(
      ModuleFinder beforeFinder, ModuleFinder afterFinder, boolean assertions, String... roots) {
    var layer =
        new ModuleLayerBuilder()
            .before(beforeFinder)
            .after(afterFinder)
            .roots(Set.of(roots))
            .build();
    if (assertions) for (var root : roots) layer.findLoader(root).setDefaultAssertionStatus(true);
    return ServiceLoader.load(layer, ToolProvider.class).stream().map(ServiceLoader.Provider::get);
  }

  default Optional<ToolProvider> findToolProvider(String name) {
    return streamToolProviders().filter(it -> it.name().equals(name)).findFirst();
  }

  default CommandResult run(Command<?> command) {
    return run(command, command.toDescription(MAX_DESCRIPTION_LENGTH));
  }

  default CommandResult run(Command<?> command, String description) {
    var name = command.name();
    var arguments = command.arguments();
    if (command instanceof ToolProvider provider) return run(provider, arguments, description);
    var provider = findToolProvider(name);
    if (provider.isEmpty()) throw new RuntimeException("No tool provider found for name: " + name);
    return run(provider.get(), arguments, description);
  }

  default CommandResults run(Command<?> command, Command<?>... commands) {
    var sequentially = false; // TODO bach().options().is(Option.RUN_COMMANDS_SEQUENTIALLY);
    var concat = Stream.concat(Stream.of(command), Stream.of(commands));
    var stream = sequentially ? concat.sequential() : concat.parallel();
    return run(stream);
  }

  default CommandResults run(Stream<? extends Command<?>> commands) {
    var parallel = commands.isParallel();
    bach().log("Stream commands %s".formatted(parallel ? "in parallel" : "sequentially"));
    var results = commands.map(this::run).toList();
    var s = results.size() == 1 ? "" : "s";
    bach().log("Collected %d command result%s".formatted(results.size(), s));
    return new CommandResults(results);
  }

  default CommandResult run(Command<?> command, ModuleFinder finder, String... roots) {
    var providers = streamToolProviders(finder, roots);
    var provider = providers.filter(it -> it.name().equals(command.name())).findFirst();
    var arguments = command.arguments();
    var description = command.toDescription(MAX_DESCRIPTION_LENGTH);
    return run(provider.orElseThrow(), arguments, description);
  }

  default CommandResult run(ToolProvider provider, List<String> arguments, String description) {
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
      var enabled = true; // TODO bach()....tools().enabled(name);
      if (enabled) bach().say("  %-8s %s".formatted(name, description));
      else bach().log("Skip " + name);
      code = enabled ? provider.run(new PrintWriter(out), new PrintWriter(err), args) : 0;
    } finally {
      currentThread.setContextClassLoader(currentLoader);
    }

    var duration = Duration.between(start, Instant.now());
    var tid = currentThread.getId();
    var output = out.toString().trim();
    var errors = err.toString().trim();
    var result = new CommandResult(name, arguments, tid, duration, code, output, errors);

    bach().logbook().log(result);
    return result;
  }
}
