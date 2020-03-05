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

package de.sormuras.bach.execution;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Executable task definition. */
public /*static*/ class Task implements Snippet.Scribe {

  private final String title;
  private final boolean parallel;
  private final List<Task> children;

  /** Initialize a task instance. */
  public Task(String title, boolean parallel, List<Task> children) {
    this.title = Objects.requireNonNull(title, "title");
    this.parallel = parallel;
    this.children = List.copyOf(children);
  }

  public String title() {
    return title;
  }

  public boolean parallel() {
    return parallel;
  }

  public List<Task> children() {
    return children;
  }

  /** Default computation called before executing child tasks. */
  public ExecutionResult execute(ExecutionContext execution) {
    return execution.ok();
  }

  @Override
  public Snippet toSnippet() {
    return Snippet.of("// " + title);
  }

  /** Visit this task and recurse into all nested tasks. */
  void walk(Consumer<Task> consumer) {
    consumer.accept(this);
    for(var task : children()) task.walk(consumer);
  }
}
