open module scaffold {
  exports scaffold.api;

  requires java.scripting;
  requires org.jooq.jool;
  requires org.apiguardian.api;
  requires org.junit.jupiter.api;

  uses scaffold.api.ScaffoldPlugin;
}
