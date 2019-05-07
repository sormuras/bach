module scaffold {
  exports scaffold.api;

  requires java.scripting;
  requires org.jooq.jool;

  uses scaffold.api.ScaffoldPlugin;
}
