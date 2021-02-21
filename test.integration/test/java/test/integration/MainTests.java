package test.integration;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import com.github.sormuras.bach.Main;
import org.junit.jupiter.api.Test;

class MainTests {

  @Test
  void help() {
    var help = Main.computeHelpMessage();
    assertLinesMatch(
        """
        Usage: bach [OPTIONS] ACTION [ACTIONS...]
                 to execute one or more actions in sequence
           or: bach [OPTIONS] [ACTIONS...] tool NAME [ARGS...]
                 to execute a provided tool with tool-specific arguments

        Options include the following flags:

          --verbose
            Print messages about what Bach is doing.
          --silent
            Mute all normal (expected) printouts.
          --version
            Print Bach's version and exit.
          --show-version
            Print Bach's version and continue.
          --help
            Print usage information and exit.
          --strict
            Activate all verification measures available.
          --run-commands-sequentially
            Prevent parallel execution of commands.

        Options include the following key-value pairs:

          --base-directory VALUE
            Specify the directory of the project, defaults to user's current directory
          --bach-info VALUE
            Specify the module to load on startup, defaults to "bach.info"
          --project-name VALUE
            Specify the name of the project.
          --project-version VALUE
            Specify the version of the project.
          --skip-tool VALUE (repeatable option)
            Skip all executions of the specified tool.

        Actions include:

          build
            Build the current project.
          clean
            Delete workspace directory.
          format
            Format source code files.
          info
            Print information about Bach and the current project.
          tool
            Run provided tool with NAME passing any following arguments.
        """
            .lines(),
        help.lines());
  }
}
