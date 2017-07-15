module hello {
  requires world;
  provides com.greetings.Greeter with hello.Hello;
}
