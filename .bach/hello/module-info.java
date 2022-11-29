import run.bach.Command;

@Command(
    name = "hi",
    args = {"hello", "one", "+", "hello", "two"})
module hello {
  requires run.bach;

  provides java.util.spi.ToolProvider with
      hello.Hello;
}
