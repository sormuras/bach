package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.MirroringStringPrintWriter;
import com.github.sormuras.bach.internal.StringSupport;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;

/** Java Shell Builder. */
public record Bach(Configuration configuration, Project project) implements ToolRunner {

  public static Bach ofDefaults() {
    return new Bach(Configuration.ofDefaults(), Project.ofDefaults());
  }

  @Override
  public void run(ToolCall call, Set<RunModifier> modifiers) {
    run(configuration.finder(), call, modifiers);
  }

  @Override
  public void run(ToolFinder finder, ToolCall call, Set<RunModifier> modifiers) {
    var name = call.name();
    var arguments = call.arguments();
    var printer = configuration.printer();
    var verbose = configuration.isVerbose() || modifiers.contains(RunModifier.VERBOSE);
    var force = modifiers.contains(RunModifier.FORCE);
    var visible = !modifiers.contains(RunModifier.HIDDEN);
    var tccl = modifiers.contains(RunModifier.RUN_WITH_PROVIDERS_CLASS_LOADER);

    var tools = finder.find(name);
    if (tools.isEmpty()) throw new ToolNotFoundException(name);
    var provider = tools.get(0).provider();
    var operator = provider instanceof ToolOperator;
    var run = force || operator || !configuration.isDryRun();

    var thread = Thread.currentThread();
    var loader = thread.getContextClassLoader();
    try {
      if (visible) {
        var flat = verbose ? name : name.substring(name.indexOf('/') + 1);
        var text = arguments.isEmpty() ? flat : flat + ' ' + StringSupport.join(arguments);
        printer.out(operator ? text : "  " + text);
      }
      if (tccl) {
        thread.setContextClassLoader(provider.getClass().getClassLoader());
      }
      if (run) {
        var code = run(provider, name, arguments);
        if (code != 0) {
          throw new RuntimeException("%s returned non-zero exit code: %d".formatted(name, code));
        }
      }
    } finally {
      thread.setContextClassLoader(loader);
    }
  }

  private int run(ToolProvider provider, String name, List<String> arguments) {
    var event = new ToolRunEvent();
    event.name = name;
    event.args = String.join(" ", arguments);
    var printer = configuration.printer();
    try (var out = new MirroringStringPrintWriter(printer.out());
        var err = new MirroringStringPrintWriter(printer.err())) {
      var args = arguments.toArray(String[]::new);
      event.begin();
      if (provider instanceof ToolOperator operator) {
        event.code = operator.run(this, out, err, args);
      } else {
        event.code = provider.run(out, err, args);
      }
      event.end();
      event.out = out.toString().strip();
      event.err = err.toString().strip();
      event.commit();
      return event.code;
    } finally {
      printer.out().flush();
      printer.err().flush();
    }
  }
}
