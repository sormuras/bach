package com.github.sormuras.bach;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.spi.ToolProvider;

/** An object that runs {@link ToolCall} instances. */
public class ToolRunner {

  private final Logger logger;
  private final Deque<ToolResponse> history;

  /** Initializes a tool shell instance with the given components. */
  public ToolRunner() {
    this.logger = System.getLogger(ToolRunner.class.getName());
    this.history = new ConcurrentLinkedDeque<>();
  }

  /**
   * Computes a string from the characters buffered in the given writer.
   *
   * @param writer the character stream
   * @return a message text
   */
  protected String computeMessageText(StringWriter writer) {
    if (writer.getBuffer().length() == 0) return "";
    return writer.toString().strip();
  }

  /**
   * Returns the history of tool calls as recorded in response objects.
   *
   * @return a double ended queue of tool response objects
   */
  public Deque<ToolResponse> history() {
    return new ArrayDeque<>(history);
  }

  /**
   * Runs a tool call.
   *
   * @param call the call to run
   * @return a tool call response object
   */
  public ToolResponse run(ToolCall call) {
    if (call instanceof ToolProvider) {
      return run((ToolProvider) call, call.args());
    }
    return run(call.name(), call.args());
  }

  /**
   * Runs a tool call.
   *
   * @param name the name of the tool to run
   * @param args the arguments
   * @return a tool call response object
   */
  public ToolResponse run(String name, String... args) {
    return run(ToolProvider.findFirst(name).orElseThrow(), args);
  }

  ToolResponse run(ToolProvider provider, String... args) {
    var output = new StringWriter();
    var outputPrintWriter = new PrintWriter(output);
    var errors = new StringWriter();
    var errorsPrintWriter = new PrintWriter(errors);
    var name = provider.name();
    logger.log(Level.TRACE, "Run " + name + " with " + args.length + " argument(s)");

    var start = Instant.now();
    var code = provider.run(outputPrintWriter, errorsPrintWriter, args);
    var duration = Duration.between(start, Instant.now());

    var thread = Thread.currentThread().getId();
    var out = computeMessageText(output);
    var err = computeMessageText(errors);
    var response = new ToolResponse(name, args, thread, duration, code, out, err);
    logger.log(Level.DEBUG, response.toString());

    history.add(response);
    return response;
  }
}
