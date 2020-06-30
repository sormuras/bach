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

import de.sormuras.bach.project.Project;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;

/** Bach - Java Shell Builder. */
public final class Bach {

  /** Version of the Java Shell Builder. */
  public static final Version VERSION = Version.parse("14-ea");

  /**
   * Main entry-point.
   *
   * @param args the arguments
   */
  public static void main(String... args) {
    Main.main(args);
  }

  public static Bach ofSystem() {
    return new Bach(Flags.ofSystem(), Logbook.ofSystem(), Project.ofSystem(), Builder::new);
  }

  public Bach with(Flags flags) {
    return new Bach(flags, logbook, project, builder);
  }

  public Bach with(Logbook logbook) {
    return new Bach(flags, logbook, project, builder);
  }

  public Bach with(Project project) {
    return new Bach(flags, logbook, project, builder);
  }

  public Bach with(Builder.Factory workflow) {
    return new Bach(flags, logbook, project, workflow);
  }

  public Bach with(Flag... flags) {
    return with(flags().with(flags));
  }

  public Bach without(Flag... flags) {
    return with(flags().without(flags));
  }

  public Bach with(Level threshold) {
    return with(logbook().with(threshold));
  }

  private final Flags flags;
  private final Logbook logbook;
  private final Project project;
  private final Builder.Factory builder;

  public Bach(Flags flags, Logbook logbook, Project project, Builder.Factory builder) {
    this.flags = flags;
    this.logbook = logbook;
    this.project = project;
    this.builder = builder;
  }

  public Flags flags() {
    return flags;
  }

  public Logbook logbook() {
    return logbook;
  }

  public Project project() {
    return project;
  }

  public void buildProject() {
    logbook.log(Level.TRACE, toString());
    logbook.log(Level.TRACE, "\tflags.set=%s", flags.set());
    logbook.log(Level.TRACE, "\tlogbook.threshold=%s", logbook.threshold());
    logbook.log(Level.DEBUG, "Build of %s started", project.toNameAndVersion());
    try {
      var start = Instant.now();
      builder.create(this).build();
      var duration = Duration.between(start, Instant.now()).toMillis();
      logbook.log(Level.INFO, "Build of %s took %d ms", project.toNameAndVersion(), duration);
    } catch (Exception exception) {
      var message = logbook.log(Level.ERROR, "build failed throwing %s", exception);
      if (flags.isFailOnError()) throw new AssertionError(message, exception);
    } finally {
      writeLogbook();
    }
  }

  public void executeCall(Call<?> call) {
    logbook.log(Level.INFO, call.toCommandLine());

    var provider = call.findProvider();
    if (provider.isEmpty()) {
      var message = logbook.log(Level.ERROR, "Tool provider with name '%s' not found", call.name());
      if (flags.isFailFast()) throw new AssertionError(message);
      return;
    }

    if (flags.isDryRun()) return;

    var tool = provider.get();
    var currentThread = Thread.currentThread();
    var currentContextLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(tool.getClass().getClassLoader());
    var out = new StringWriter();
    var err = new StringWriter();
    var args = call.toStringArray();
    var start = Instant.now();

    try {
      var code = tool.run(new PrintWriter(out), new PrintWriter(err), args);

      var duration = Duration.between(start, Instant.now());
      var normal = out.toString().strip();
      var errors = err.toString().strip();
      var result = logbook.print(call, normal, errors, duration, code);
      logbook.log(Level.DEBUG, "%s finished after %d ms", tool.name(), duration.toMillis());

      if (code == 0) return;

      var caption = logbook.log(Level.ERROR, "%s failed with exit code %d", tool.name(), code);
      var message = new StringJoiner(System.lineSeparator());
      message.add(caption);
      result.toStrings().forEach(message::add);
      if (flags.isFailFast()) throw new AssertionError(message);
    } catch (RuntimeException exception) {
      logbook.log(Level.ERROR, "%s failed throwing %s", tool.name(), exception);
      if (flags.isFailFast()) throw exception;
    } finally {
      currentThread.setContextClassLoader(currentContextLoader);
    }
  }

  public void writeLogbook() {
    try {
      var path = project.base().workspace("logbook.md");
      Files.write(path, logbook.toMarkdown(project));
      logbook.log(Level.INFO, "Wrote logbook to %s", path.toUri());
    } catch (Exception exception) {
      var message = logbook.log(Level.ERROR, "write logbook failed: %s", exception);
      if (flags.isFailOnError()) throw new AssertionError(message, exception);
    }
  }

  @Override
  public String toString() {
    return "Bach.java " + VERSION;
  }

  /** A flag represents a feature toggle. */
  public enum Flag {
    DRY_RUN(false),
    FAIL_FAST(true),
    FAIL_ON_ERROR(true);

    private final boolean enabledByDefault;

    Flag(boolean enabledByDefault) {
      this.enabledByDefault = enabledByDefault;
    }

    public boolean isEnabledByDefault() {
      return enabledByDefault;
    }
  }

  /** A set of modifiers and feature toggles. */
  public static class Flags {

    public static Flags ofSystem() {
      var flags = new TreeSet<Flag>();
      for (var flag : Flag.values()) {
        var key = "bach." + flag.name().toLowerCase().replace('_', '-');
        var property = System.getProperty(key, flag.isEnabledByDefault() ? "true" : "false");
        if (Boolean.parseBoolean(property)) flags.add(flag);
      }
      return new Flags(flags);
    }

    private final Set<Flag> set;

    public Flags(Set<Flag> set) {
      this.set = set.isEmpty() ? Set.of() : EnumSet.copyOf(set);
    }

    public Set<Flag> set() {
      return set;
    }

    public boolean isDryRun() {
      return set.contains(Flag.DRY_RUN);
    }

    public boolean isFailFast() {
      return set.contains(Flag.FAIL_FAST);
    }

    public boolean isFailOnError() {
      return set.contains(Flag.FAIL_ON_ERROR);
    }

    public Flags with(Set<Flag> set) {
      return new Flags(set);
    }

    public Flags with(Flag... additionalFlags) {
      var flags = new TreeSet<>(set);
      flags.addAll(Set.of(additionalFlags));
      return with(flags);
    }

    public Flags without(Flag... redundantFlags) {
      var flags = new TreeSet<>(set());
      flags.removeAll(Set.of(redundantFlags));
      return with(flags);
    }
  }
}
