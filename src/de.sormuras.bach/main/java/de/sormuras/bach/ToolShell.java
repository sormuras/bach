/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** A tool shell executes tool calls. */
public class ToolShell {

  /** A feature flag of the tool shell implementation. */
  public enum Flag {
    /** Store all tool call response objects. */
    RECORD_HISTORY,

    /** Verify that the last tool call returned {@code 0} as its exit code. */
    FAIL_FAST;

    /** The default set of feature flag that contains all feature flag constants. */
    public static final Set<Flag> DEFAULTS = EnumSet.allOf(Flag.class);
  }

  private final Logger logger;
  private final Set<Flag> flags;
  private final Deque<ToolResponse> history;

  /** Initializes a default tool shell instance. */
  public ToolShell() {
    this(System.getLogger(ToolShell.class.getName()), Flag.DEFAULTS);
  }

  /** Initializes a tool shell instance with the given components. */
  public ToolShell(Logger logger, Set<Flag> flags) {
    this.logger = logger;
    this.flags = flags.isEmpty() ? Set.of() : EnumSet.copyOf(flags);
    this.history = new ConcurrentLinkedDeque<>();
  }

  /**
   * Return the feature flag set.
   *
   * @return a set of feature flag constants
   */
  public Set<Flag> getFlags() {
    return new TreeSet<>(flags);
  }

  /**
   * Return the history of tool calls as recorded in response objects.
   *
   * @return an unmodifiable collection of tool response objects
   */
  public Deque<ToolResponse> getHistory() {
    return new ArrayDeque<>(history);
  }

  protected ToolProvider computeToolProvider(ToolCall call) {
    if (call instanceof ToolProvider) return (ToolProvider) call;
    return computeToolProvider(call.name());
  }

  protected ToolProvider computeToolProvider(String name) {
    var contextLoader = Thread.currentThread().getContextClassLoader();
    var serviceLoader = ServiceLoader.load(ToolProvider.class, contextLoader);
    return StreamSupport.stream(serviceLoader.spliterator(), false)
        .filter(provider -> provider.name().equals(name))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("No provider for: " + name));
  }

  protected String computeMessageText(StringWriter writer) {
    if (writer.getBuffer().length() == 0) return "";
    return writer.toString().strip();
  }

  /**
   * Run the given tool call.
   *
   * @param name the name of the tool to call
   * @param args the arguments of this call
   */
  public void call(String name, String... args) {
    call(computeToolProvider(name), args);
  }

  /**
   * Run the given tool call.
   *
   * @param call the tool call to run
   */
  public void call(ToolCall call) {
    call(computeToolProvider(call), call.args());
  }

  /**
   * Run all given tool calls concurrently.
   *
   * @param call one tool call to run
   * @param more more tool calls to run in parallel
   */
  public void call(ToolCall call, ToolCall... more) {
    Stream.concat(Stream.of(call), Stream.of(more)).parallel().forEach(this::call);
  }

  /**
   * Run all given tool calls concurrently.
   *
   * @param calls tool calls to run in parallel
   */
  public void call(Iterable<ToolCall> calls) {
    StreamSupport.stream(calls.spliterator(), true).forEach(this::call);
  }

  /**
   * Run the given tool provider with the given arguments.
   *
   * @param provider the tool provider to run
   * @param args the arguments of this call
   */
  public ToolResponse call(ToolProvider provider, String... args) {
    var currentThread = Thread.currentThread();
    var currentLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(provider.getClass().getClassLoader());
    try {
      return call(currentThread, provider, args);
    } finally {
      currentThread.setContextClassLoader(currentLoader);
    }
  }

  private ToolResponse call(Thread thread, ToolProvider provider, String... args) {
    var output = new StringWriter();
    var outputPrintWriter = new PrintWriter(output);
    var errors = new StringWriter();
    var errorsPrintWriter = new PrintWriter(errors);
    var name = provider.name();
    logger.log(Level.TRACE, "Calling " + name + " with " + args.length + " argument(s)");

    var start = Instant.now();
    var code = provider.run(outputPrintWriter, errorsPrintWriter, args);
    var duration = Duration.between(start, Instant.now());

    var out = computeMessageText(output);
    var err = computeMessageText(errors);
    var response = new ToolResponse(name, args, thread.getId(), duration, code, out, err);
    logger.log(Level.DEBUG, response.toString());

    var discardHistory = !flags.contains(Flag.RECORD_HISTORY);
    if (discardHistory) history.clear();
    history.add(response); // always store the "last" response

    if (flags.contains(Flag.FAIL_FAST)) response.checkSuccessful();
    return response;
  }

  public void checkHistoryForErrors() {
    var errors = history.stream().filter(ToolResponse::isError).collect(Collectors.toList());
    logger.log(Level.DEBUG, errors.size() + " error(s) found in " + history.size() + " responses");
    if (errors.isEmpty()) return;
    throw new RuntimeException("Response history contains at least one error: " + errors);
  }
}
