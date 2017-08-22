module hallo {
  requires world;

  provides com.greetings.Greeter with
      hallo.Hallo;
}
