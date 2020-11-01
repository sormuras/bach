package com.github.sormuras.bach;

import com.github.sormuras.bach.module.ModuleDirectory;
import com.github.sormuras.bach.tool.Command;
import com.github.sormuras.bach.tool.ToolCall;
import com.github.sormuras.bach.tool.ToolResponse;
import com.github.sormuras.bach.tool.ToolRunner;

/** Tool-related API. */
public /*sealed*/ interface Tool extends Print /*permits Bach*/ {

  /**
   * Run the given call using the directory to find its tool provider.
   *
   * @param directory the module finder to query for already loaded modules
   * @param call the name and arguments of the tool to run
   * @return a responce object describing the result of the tool run
   */
  default ToolResponse toolCall(ModuleDirectory directory, ToolCall call) {
    return new ToolRunner(directory.finder()).run(call);
  }

  /**
   * Run the tool using the directory to find it and passing the given arguments.
   *
   * @param directory the module finder to query for already loaded modules
   * @param name the name of the tool to run
   * @param args the array of args to be passed to the tool as strings
   */
  default void toolRun(ModuleDirectory directory, String name, Object... args) {
    var response = toolCall(directory, Command.of(name, args));
    if (!response.out().isEmpty()) printStream().println(response.out());
    printStream().println(response.err());
  }
}
