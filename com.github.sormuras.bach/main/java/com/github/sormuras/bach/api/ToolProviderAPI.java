package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Recording;
import com.github.sormuras.bach.internal.ModuleLayerBuilder;
import java.lang.module.ModuleFinder;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/** Methods related to finding and running provided tools. */
public interface ToolProviderAPI {

  Bach bach();

  default Stream<ToolProvider> computeToolProviders() {
    var externals = bach().base().externals();
    var layer = new ModuleLayerBuilder().before(ModuleFinder.of(externals)).build();
    return ServiceLoader.load(layer, ToolProvider.class).stream().map(ServiceLoader.Provider::get);
  }

  default ToolProvider computeToolProvider(String name) {
    var provider = computeToolProviders().filter(it -> it.name().equals(name)).findFirst();
    if (provider.isPresent()) return provider.get();
    throw new RuntimeException("No tool provider found for name: " + name);
  }

  Recording run(ToolProvider provider, List<String> arguments);

  default Recording run(Command<?> command) {
    bach().debug("Run %s", command.toLine());
    var provider = computeToolProvider(command.name());
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
}
