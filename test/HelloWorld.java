import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

public class HelloWorld {
  public static void main(String[] args) {
    JShellBuilder jsb =
        new JShellBuilder.Builder()
            .name("hello")
            .level(Level.FINER)
            .handler(new ConsoleHandler())
            .build();

    jsb.call("javac", "--version");
    jsb.build();
    jsb.call("java", "--version");
  }
}
