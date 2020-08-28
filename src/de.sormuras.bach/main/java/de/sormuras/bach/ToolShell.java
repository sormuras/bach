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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** A tool shell runs tool calls. */
public class ToolShell {

  private final Map<String, ToolProvider> providers;
  private final Consumer<ToolResponse> listener;
  private final Deque<ToolResponse> history;

  public ToolShell() {
    this(Map.of(), ToolResponse::requireSuccessful);
  }

  public ToolShell(Map<String, ToolProvider> providers, Consumer<ToolResponse> listener) {
    this.providers = providers;
    this.listener = listener;
    this.history = new ConcurrentLinkedDeque<>();
  }

  /**
   * Return the history of tool calls as recorded in response objects.
   *
   * @return an unmodifiable collection of tool response objects
   */
  public Deque<ToolResponse> getHistory() {
    return new ArrayDeque<>(history);
  }

  private ToolProvider computeToolProvider(ToolCall call) {
    if (call instanceof ToolProvider) return (ToolProvider) call;
    return computeToolProvider(call.name());
  }

  private ToolProvider computeToolProvider(String name) {
    if (providers.containsKey(name)) return providers.get(name);
    return ToolProvider.findFirst(name).orElseThrow();
  }

  private String computeMessageText(StringWriter writer) {
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
    run(computeToolProvider(name), args);
  }

  /**
   * Run the given tool call.
   *
   * @param call the tool call to run
   */
  public void call(ToolCall call) {
    run(computeToolProvider(call), call.args());
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

  private void run(ToolProvider provider, String... args) {
    var name = provider.name();
    var currentThread = Thread.currentThread();
    var currentLoader = currentThread.getContextClassLoader();
    var output = new StringWriter();
    var errors = new StringWriter();
    try {
      currentThread.setContextClassLoader(provider.getClass().getClassLoader());
      var start = Instant.now();
      var code = provider.run(new PrintWriter(output), new PrintWriter(errors), args);
      var duration = Duration.between(start, Instant.now());
      var thread = currentThread.getId();
      var out = computeMessageText(output);
      var err = computeMessageText(errors);
      var response = new ToolResponse(name, args, thread, duration, code, out, err);
      history.add(response);
      listener.accept(response);
    } finally {
      currentThread.setContextClassLoader(currentLoader);
    }
  }
}
