package run.bach.internal;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

public record NativeProcessToolProvider(String name, List<String> command) implements ToolProvider {
  @Override
  public int run(PrintWriter out, PrintWriter err, String... arguments) {
    var builder = new ProcessBuilder(new ArrayList<>(command));
    builder.command().addAll(List.of(arguments));
    try {
      var process = builder.start();
      new Thread(new LinePrinter(process.getInputStream(), out)).start();
      new Thread(new LinePrinter(process.getErrorStream(), err)).start();
      return process.waitFor();
    } catch (InterruptedException exception) {
      return -1;
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return 1;
    }
  }

  record LinePrinter(InputStream stream, PrintWriter writer) implements Runnable {
    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(stream)).lines().forEach(writer::println);
    }
  }
}
