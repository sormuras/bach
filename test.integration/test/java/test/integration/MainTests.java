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
          --run-commands-sequentially
            Prevent parallel execution of commands.

        Options include:

          --configuration MODULE
            Specify the module that is normally annotated with @ProjectInfo or provides
            a custom Bach.Factory implementation, defaults to "configuration".

        Project-related options include:

          --project-name NAME
            Specify the name of the project, defaults to "noname".
          --project-version VERSION
            Specify the version of the project, defaults to "0".

        Actions include:

          build
            Build the current project.
          clean
            Delete workspace directory.
          info
            Print information about Bach and the current project.
          tool
            Run provided tool with NAME passing any following arguments.
        """
            .lines(),
        help.lines());
  }
}
