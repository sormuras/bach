open module application.api {

  // main

  exports application.api;

  uses application.api.Plugin;

  provides application.api.Plugin with
      application.api.internal.Reverse;

  // test

  requires org.junit.jupiter.api;
}
