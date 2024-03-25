/*
 * Copyright (c) 2024 Christian Stein
 * Licensed under the Universal Permissive License v 1.0 -> https://opensource.org/license/upl
 */

package run.bach;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A finder of tools.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * ToolFinder.compose(
 *     ToolFinder.of("jar", "javac", "javadoc"),
 *     ToolFinder.of("java", "jfr")
 * )
 * }</pre>
 */
@FunctionalInterface
public interface ToolFinder {
  List<Tool> tools();

  /**
   * {@return an instance of a tool for the given tool-identifying name}
   *
   * @param name the name of the tool to lookup
   */
  default Optional<Tool> findTool(String name) {
    return tools().stream().filter(tool -> tool.identifier().matches(name)).findFirst();
  }

  /**
   * {@return an instance of a tool for the given tool-identifying name}
   *
   * @param name the name of the tool to lookup
   * @throws ToolNotFoundException when a tool could not be found the given name
   */
  default Tool findToolOrElseThrow(String name) {
    var tool = findTool(name);
    if (tool.isPresent()) return tool.get();
    throw  new ToolNotFoundException("Tool named `%s` not found ".formatted(name));
  }

  /**
   * {@return a tool finder composed all tools specified by their names}
   *
   * @param tools the names of the tools to be looked-up
   * @throws ToolNotFoundException if any tool could not be found
   */
  static ToolFinder of(String... tools) {
    return of(Stream.of(tools).map(Tool::of).toArray(Tool[]::new));
  }

  static ToolFinder of(Tool... tools) {
    return new DefaultFinder(List.of(tools));
  }

  static ToolFinder ofSystem() {
    return new SystemFinder();
  }

  static ToolFinder compose(ToolFinder... finders) {
    return new CompositeFinder(List.of(finders));
  }

  record CompositeFinder(List<ToolFinder> finders) implements ToolFinder {
    @Override
    public List<Tool> tools() {
      return finders.stream().flatMap(finder -> finder.tools().stream()).toList();
    }
  }

  record DefaultFinder(List<Tool> tools) implements ToolFinder {}

  record SystemFinder() implements ToolFinder {
    @Override
    public List<Tool> tools() {
      return List.of();
    }

    @Override
    public Optional<Tool> findTool(String name) {
      try {
        return Optional.of(Tool.of(name));
      } catch (ToolNotFoundException exception) {
        return Optional.empty();
      }
    }
  }
}
