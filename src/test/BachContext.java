/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

class BachContext implements ParameterResolver {

  class Recorder implements BiConsumer<System.Logger.Level, String> {

    class Entry {
      final System.Logger.Level level;
      final String message;

      Entry(System.Logger.Level level, String message) {
        this.level = level;
        this.message = message;
      }
    }

    List<String> all = new CopyOnWriteArrayList<>();
    List<Entry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void accept(System.Logger.Level level, String message) {
      all.add(message);
      entries.add(new Entry(level, message));
    }

    List<String> level(System.Logger.Level level) {
      return entries.stream()
          .filter(e -> e.level.getSeverity() >= level.getSeverity())
          .map(e -> e.message)
          .collect(Collectors.toList());
    }
  }

  final Bach bach;
  final Recorder recorder;

  ByteArrayOutputStream bytesErr = new ByteArrayOutputStream(2000);
  ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(2000);

  BachContext() {
    this(new Bach());
  }

  BachContext(Bach bach) {
    this.bach = bach;
    this.recorder = new Recorder();

    bach.log.logger = recorder;
    bach.var.streamErr = new PrintStream(bytesErr);
    bach.var.streamOut = new PrintStream(bytesOut);
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext unused) {
    var type = parameterContext.getParameter().getType();
    return type.equals(getClass()) || type.equals(Bach.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext unused)
      throws ParameterResolutionException {
    var context = new BachContext();
    var type = parameterContext.getParameter().getType();
    if (type.equals(getClass())) {
      return context;
    }
    if (type.equals(Bach.class)) {
      return context.bach;
    }
    throw new ParameterResolutionException("Can't resolve parameter of type: " + type);
  }

  Stream<Supplier<Integer>> tasks(int size) {
    return IntStream.rangeClosed(1, size).boxed().map(i -> () -> task("" + i));
  }

  int task(String name) {
    return task(name, () -> 0);
  }

  int task(String name, IntSupplier result) {
    bach.log.info("%s begin", name);
    var millis = (long) (Math.random() * 200 + 50);
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.interrupted();
    }
    bach.log.info("%s done. %s", name, millis);
    return result.getAsInt();
  }
}
