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

package test.modules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.sormuras.bach.ToolCall;
import de.sormuras.bach.ToolShell;
import java.io.PrintWriter;
import java.util.Set;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ToolCallTests {

  @Test
  void echo12345() {
    var shell = new ToolShell();
    assertEquals(0, shell.getHistory().size());
    shell.call(echo("1"));
    shell.call(echo("2"));
    shell.call(echo("3"));
    assertEquals(3, shell.getHistory().size());
    shell.call(echo("4"), echo("5"));
    assertEquals(5, shell.getHistory().size());
  }

  @Test
  void sleep() {
    var shell = new ToolShell();
    assertEquals(0, shell.getHistory().size());
    shell.call(Set.of(new Sleep(10), new Sleep(20), new Sleep(30)));
    assertEquals(3, shell.getHistory().size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"jar", "javac", "javadoc", "jlink"})
  void versions(String tool) {
    var shell = new ToolShell();
    shell.call(tool, "--version");
    var response = shell.getHistory().getLast();
    assertTrue(response.toString().contains("" + Runtime.version().feature()));
  }

  public static Echo echo(String message) {
    return new Echo(true, message, 0);
  }

  public static final class Echo implements ToolCall, ToolProvider {

    private final boolean normal;
    private final String message;
    private final int code;

    public Echo(boolean normal, String message, int code) {
      this.normal = normal;
      this.message = message;
      this.code = code;
    }

    @Override
    public String name() {
      return "echo";
    }

    @Override
    public String[] args() {
      return new String[] {message};
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      (normal ? out : err).print(message);
      return code;
    }
  }

  private static final class Sleep implements ToolCall, ToolProvider {

    private final long millis;

    private Sleep(long millis) {
      this.millis = millis;
    }

    @Override
    public String name() {
      return "sleep";
    }

    @Override
    public String[] args() {
      return new String[] {Long.toString(millis)};
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      try {
        Thread.sleep(millis);
        return 0;
      } catch (InterruptedException e) {
        Thread.interrupted();
        return 1;
      }
    }
  }
}
