/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.spi.ToolProvider;

/**
 * An implementation of the tool provider interface running operating system programs.
 *
 * @param name the name of this tool program
 * @param command the list containing the executable program and its fixed arguments
 * @param processBuilderTweaker the tweaker of the process builder instance created before starting
 * @param processWaiter the waiter waits for the started process to finish and returns an exit value
 * @see ToolProvider#name()
 */
public record ToolProgram(
    String name,
    List<String> command,
    ProcessBuilderTweaker processBuilderTweaker,
    ProcessWaiter processWaiter)
    implements ToolProvider {

  /**
   * {@return an instance of a tool program launching a Java application via the {@code java} tool}
   *
   * <p>Example:
   *
   * <pre>{@code
   * var base = ToolProgram.java("--limit-modules", "java.base");
   * Tool.of(base).call("--list-modules").run();
   * }</pre>
   *
   * @param args zero or more fixed arguments
   * @see <a href="https://docs.oracle.com/en/java/javase/22/docs/specs/man/java.html">The java
   *     Command</a>
   */
  public static ToolProgram java(String... args) {
    var name = "java";
    var tool = findJavaDevelopmentKitTool(name, args);
    if (tool.isPresent()) return tool.get();
    throw new ToolNotFoundException("Tool not found for name: " + name);
  }

  /**
   * {@return an instance of a JDK tool program for the given name, if found}
   *
   * @param name the name of the JDK tool program to look up
   * @param args the fixed arguments
   * @see <a href="https://docs.oracle.com/en/java/javase/22/docs/specs/man/">Java® Development Kit
   *     Version 22 Tool Specifications</a>
   */
  public static Optional<ToolProgram> findJavaDevelopmentKitTool(String name, String... args) {
    var bin = Path.of(System.getProperty("java.home", ""), "bin");
    return findInFolder(name, bin, args);
  }

  /**
   * {@return an operating system program for the given name in the specified folder, if found}
   *
   * @param name the name of the operating system program to lookup
   * @param folder the directory to look up the name in
   * @param args the fixed arguments
   */
  public static Optional<ToolProgram> findInFolder(String name, Path folder, String... args) {
    if (!Files.isDirectory(folder)) return Optional.empty();
    var win = System.getProperty("os.name", "").toLowerCase().startsWith("win");
    var file = name + (win && !name.endsWith(".exe") ? ".exe" : "");
    return findExecutable(name, folder.resolve(file), args);
  }

  /**
   * {@return an operating system program for the given file path, if it is executable}
   *
   * @param name the name of the operating system program to lookup
   * @param file the file to look up the name in
   * @param args the fixed arguments
   * @see Files#isExecutable(Path)
   */
  public static Optional<ToolProgram> findExecutable(String name, Path file, String... args) {
    if (!Files.isExecutable(file)) return Optional.empty();
    var command = new ArrayList<String>();
    command.add(file.toString());
    command.addAll(List.of(args));
    return Optional.of(new ToolProgram(name, List.copyOf(command)));
  }

  public ToolProgram(String name, List<String> command) {
    this(name, command, x -> x, Process::waitFor);
  }

  public ToolProgram withProcessBuilderTweaker(ProcessBuilderTweaker processBuilderTweaker) {
    return new ToolProgram(name, command, processBuilderTweaker, processWaiter);
  }

  public ToolProgram withProcessWaiter(ProcessWaiter processWaiter) {
    return new ToolProgram(name, command, processBuilderTweaker, processWaiter);
  }

  public Tool tool() {
    var path = Path.of(command.getFirst()).normalize();
    var namespace = path.getNameCount() == 0 ? "" : path.getParent().toString().replace('\\', '/');
    var file = path.getFileName() == null ? "<null>" : path.getFileName().toString();
    var name = file.endsWith(".exe") ? file.substring(0, file.length() - 4) : file;
    return new Tool(Tool.Identifier.of(namespace, name, null), this);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... arguments) {
    var processBuilder = new ProcessBuilder(new ArrayList<>(command));
    processBuilder.command().addAll(List.of(arguments));
    try {
      var process = processBuilderTweaker.tweak(processBuilder).start();
      var threadBuilder = Thread.ofVirtual();
      threadBuilder.name(name + "-out").start(new LinePrinter(process.getInputStream(), out));
      threadBuilder.name(name + "-err").start(new LinePrinter(process.getErrorStream(), err));
      return process.isAlive() ? processWaiter().waitFor(process) : process.exitValue();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return -1;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }

  @FunctionalInterface
  public interface ProcessBuilderTweaker {
    ProcessBuilder tweak(ProcessBuilder builder);
  }

  @FunctionalInterface
  public interface ProcessWaiter {
    int waitFor(Process process) throws InterruptedException;
  }

  private record LinePrinter(InputStream stream, PrintWriter writer) implements Runnable {
    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(stream)).lines().forEach(writer::println);
    }
  }
}
