package bach.info;

import com.github.sormuras.bach.ToolCall;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;

public record MyJavaCall(List<String> arguments) implements ToolCall<MyJavaCall>, ToolProvider {

  @Override
  public MyJavaCall arguments(List<String> arguments) {
    return new MyJavaCall(arguments);
  }

  @Override
  public String name() {
    return "java";
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    var builder = new ProcessBuilder("java");
    builder.command().addAll(arguments());
    try {
      var process = builder.start();
      new Thread(new StreamGobbler(process.getInputStream(), out::println)).start();
      new Thread(new StreamGobbler(process.getErrorStream(), err::println)).start();
      return process.waitFor();
    } catch (Exception exception) {
      exception.printStackTrace(err);
      return -1;
    }
  }

  record StreamGobbler(InputStream stream, Consumer<String> consumer) implements Runnable {
    public void run() {
      new BufferedReader(new InputStreamReader(stream)).lines().forEach(consumer);
    }
  }
}
