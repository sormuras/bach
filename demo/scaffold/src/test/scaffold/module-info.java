open module scaffold {
  exports scaffold.api;

  requires org.apiguardian.api;
  requires org.junit.jupiter.api;

  uses scaffold.api.ScaffoldPlugin;
}
