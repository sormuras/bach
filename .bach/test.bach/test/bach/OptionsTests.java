package test.bach;

import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.spi.ToolProvider;
import jdk.jfr.Enabled;
import jdk.jfr.Registered;
import run.bach.Options;

@Registered
@Enabled
public class OptionsTests implements ToolProvider {
  @Override
  public String name() {
    return getClass().getName();
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var expected =
        List.of(
            "--verbose",
            "--printer-margin",
            "99",
            "--printer-threshold",
            Level.TRACE.name().toLowerCase(),
            "jar",
            "--version",
            "+",
            "javac",
            "--version");
    var cli = Options.of(expected.toArray(String[]::new));
    assert cli.verbose();
    assert 99 == cli.printerMargin(-1);
    assert Level.TRACE == cli.printerThreshold(Level.INFO);
    assert List.of("jar", "--version", "+", "javac", "--version").equals(List.of(cli.calls()));
    return 0;
  }
}
