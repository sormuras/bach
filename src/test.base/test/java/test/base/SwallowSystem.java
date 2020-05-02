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

package test.base;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

/** Swallow and capture texts printed to system streams. */
@Target(METHOD)
@Retention(RUNTIME)
@ResourceLock(Resources.SYSTEM_OUT)
@ResourceLock(Resources.SYSTEM_ERR)
@ExtendWith(SwallowSystem.Extension.class)
public @interface SwallowSystem {

  class Extension implements BeforeEachCallback, ParameterResolver {

    @Override
    public void beforeEach(ExtensionContext context) {
      getOrComputeStreamsIfAbsent(context);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
      var isTestMethod = context.getTestMethod().isPresent();
      var isStreamsParameter = parameterContext.getParameter().getType() == Streams.class;
      return isTestMethod && isStreamsParameter;
    }

    @Override
    public Streams resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
      return getOrComputeStreamsIfAbsent(context);
    }

    private Streams getOrComputeStreamsIfAbsent(ExtensionContext context) {
      var store = context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
      return store.getOrComputeIfAbsent(Streams.class);
    }
  }

  class Streams implements ExtensionContext.Store.CloseableResource {

    private final PrintStream standardOut, standardErr;
    private final ByteArrayOutputStream out, err;
    private final List<Runnable> shutdownHooks;

    Streams() {
      this.standardOut = System.out;
      this.standardErr = System.err;
      this.out = new ByteArrayOutputStream();
      this.err = new ByteArrayOutputStream();
      this.shutdownHooks = new ArrayList<>();
      System.setOut(new PrintStream(out));
      System.setErr(new PrintStream(err));
    }

    public void addShutdownHook(Runnable runnable) {
      shutdownHooks.add(runnable);
    }

    @Override
    public void close() {
      System.setOut(standardOut);
      System.setErr(standardErr);
      shutdownHooks.forEach(Runnable::run);
    }

    public List<String> lines() {
      return out.toString().lines().collect(Collectors.toList());
    }

    public List<String> errors() {
      return err.toString().lines().collect(Collectors.toList());
    }

    @Override
    public String toString() {
      return "out=```" + out.toString() + "```, err=```" + err.toString() + "```";
    }
  }
}
