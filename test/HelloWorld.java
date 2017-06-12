public class HelloWorld {
  public static void main(String[] args) {
    JShellBuilder jsb =
        new JShellBuilder.Builder()
            .name("hello")
            .level(java.util.logging.Level.FINER)
            // .handler(new ConsoleHandler())
            .build();

    jsb.call("javac", "--version");
    jsb.build();
    jsb.call("java", "--version");
  }
}
