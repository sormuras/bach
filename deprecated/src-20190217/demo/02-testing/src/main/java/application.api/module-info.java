module application.api {
  exports application.api;

  uses application.api.Plugin;

  provides application.api.Plugin with
      application.api.internal.Reverse;
}
