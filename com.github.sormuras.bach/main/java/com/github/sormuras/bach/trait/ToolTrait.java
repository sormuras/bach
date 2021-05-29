package com.github.sormuras.bach.trait;

import com.github.sormuras.bach.ToolRun;
import com.github.sormuras.bach.ToolRuns;
import com.github.sormuras.bach.Trait;
import com.github.sormuras.bach.ToolCall;
import com.github.sormuras.bach.api.BachException;
import com.github.sormuras.bach.internal.ModuleLayerBuilder;
import com.github.sormuras.bach.tool.AnyCall;
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

public /*sealed*/ interface ToolTrait extends Trait {

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

  default ToolRun run(String tool, Object... arguments) {
    return run(new AnyCall(tool).withAll(arguments));
  }

  default ToolRun run(ToolCall<?> call) {
    return run(call, call.toDescription(MAX_DESCRIPTION_LENGTH));
  }

  default ToolRun run(ToolCall<?> call, String description) {
    var name = call.name();
    var arguments = call.arguments();
    if (call instanceof ToolProvider provider) return run(provider, arguments, description);
    var provider = findToolProvider(name);
    if (provider.isEmpty()) throw new BachException("No tool provider found for name: %s", name);
    return run(provider.get(), arguments, description);
  }

  default ToolRuns run(ToolCall<?> call, ToolCall<?>... calls) {
    return run(Stream.concat(Stream.of(call), Stream.of(calls)));
  }

  default ToolRuns run(Stream<? extends ToolCall<?>> calls) {
    var sequentially = bach().options().run_commands_sequentially();
    var stream = sequentially ? calls.sequential() : calls;
    var parallel = stream.isParallel();
    bach().log("Stream tool calls %s".formatted(parallel ? "in parallel" : "sequentially"));
    var runs = stream.map(this::run).toList();
    var s = runs.size() == 1 ? "" : "s";
    bach().log("Collected %d tool call run%s".formatted(runs.size(), s));
    return new ToolRuns(runs);
  }

  default ToolRun run(ToolCall<?> call, ModuleFinder finder, String... roots) {
    var providers = streamToolProviders(finder, roots);
    var provider = providers.filter(it -> it.name().equals(call.name())).findFirst();
    var arguments = call.arguments();
    var description = call.toDescription(MAX_DESCRIPTION_LENGTH);
    return run(provider.orElseThrow(), arguments, description);
  }

  default ToolRun run(ToolProvider provider, List<String> arguments, String description) {
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
      var dry = bach().options().dry_run();
      var enabled = !dry && bach().project().tools().enabled(name);
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
    var run = new ToolRun(name, arguments, tid, duration, code, output, errors);

    bach().logbook().log(run);
    return run;
  }
}
