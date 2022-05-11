package project;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.ToolOperator;
import java.io.PrintWriter;

public class Build implements ToolOperator {
  @Override
  public int run(Bach bach, PrintWriter out, PrintWriter err, String... args) {
    bach.run("world", System.getProperty("user.name", "?")); // provided by this module

    bach.run("com.github.sormuras.bach/build", args); // run overridden default build tool

    bach.run("hello"); // provided by an external module, needs "cache" call
    return 0;
  }
}
