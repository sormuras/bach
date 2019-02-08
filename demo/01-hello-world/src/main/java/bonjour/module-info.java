module bonjour {
  requires world;

  provides com.greetings.Greeter with
      salut.Salut;
}
