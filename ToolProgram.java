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
 * @see ToolProvider#name()
 */
public record ToolProgram(String name, List<String> command) implements ToolProvider {
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

  @Override
  public int run(PrintWriter out, PrintWriter err, String... arguments) {
    var builder = new ProcessBuilder(new ArrayList<>(command));
    builder.command().addAll(List.of(arguments));
    try {
      var process = builder.start();
      var threads = Thread.ofVirtual();
      threads.name(name + "-out").start(new LinePrinter(process.getInputStream(), out));
      threads.name(name + "-err").start(new LinePrinter(process.getErrorStream(), err));
      return process.waitFor();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return -1;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }

  private record LinePrinter(InputStream stream, PrintWriter writer) implements Runnable {
    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(stream)).lines().forEach(writer::println);
    }
  }
}
