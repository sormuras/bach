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

package de.sormuras.bach;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.TRACE;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/*BODY*/
/** Java Shell Builder. */
public class Bach {

  /** Version is either {@code master} or {@link Runtime.Version#parse(String)}-compatible. */
  public static final String VERSION = "master";

  /** Convenient short-cut to {@code "user.home"} as a path. */
  static final Path USER_HOME = Path.of(System.getProperty("user.home"));

  /** Convenient short-cut to {@code "user.dir"} as a path. */
  static final Path USER_PATH = Path.of(System.getProperty("user.dir"));

  /** Main entry-point making use of {@link System#exit(int)} on error. */
  public static void main(String... arguments) {
    var bach = new Bach();
    var args = List.of(arguments);
    var code = bach.main(args);
    if (code != 0) {
      System.err.printf("Bach main(%s) failed with error code: %d%n", args, code);
      System.exit(code);
    }
  }

  final Project project = new Project("bach", VERSION);
  final Run run;

  public Bach() {
    this(Run.system());
  }

  public Bach(Run run) {
    this.run = run;
    run.log(DEBUG, "%s initialized", this);
    run.logState(TRACE);
  }

  void help() {
    run.log(TRACE, "Bach::help()");
    run.out.println("Usage of Bach.java (" + VERSION + "):  java Bach.java [<action>...]");
    run.out.println("Available default actions are:");
    for (var action : Action.Default.values()) {
      var name = action.name().toLowerCase();
      var text =
          String.format(" %-9s    ", name) + String.join('\n' + " ".repeat(14), action.description);
      text.lines().forEach(run.out::println);
    }
  }

  /** Main entry-point, by convention, a zero status code indicates normal termination. */
  int main(List<String> arguments) {
    run.log(TRACE, "Bach::main(%s)", arguments);
    List<Action> actions;
    try {
      actions = Action.of(arguments);
      run.log(DEBUG, "actions = " + actions);
    } catch (IllegalArgumentException e) {
      run.log(ERROR, "Converting arguments to actions failed: " + e);
      return 1;
    }
    if (run.dryRun) {
      run.log(INFO, "Dry-run ends here.");
      return 0;
    }
    run(actions);
    return 0;
  }

  /** Execute a collection of actions sequentially on this instance. */
  int run(Collection<? extends Action> actions) {
    run.log(TRACE, "Bach::run(%s)", actions);
    run.log(DEBUG, "Performing %d action(s)...", actions.size());
    for (var action : actions) {
      try {
        run.log(TRACE, ">> %s", action);
        action.perform(this);
        run.log(TRACE, "<< %s", action);
      } catch (Exception exception) {
        run.log(ERROR, "Action %s threw: %s", action, exception);
        if (run.debug) {
          exception.printStackTrace(run.err);
        }
        return 1;
      }
    }
    return 0;
  }

  @Override
  public String toString() {
    return "Bach (" + VERSION + ")";
  }
}
