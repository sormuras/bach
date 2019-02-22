open module integration {
  requires scaffold;
  requires org.apiguardian.api;
  requires org.junit.jupiter.api;

  provides scaffold.api.ScaffoldPlugin with
      integration.EchoPlugin;
}
