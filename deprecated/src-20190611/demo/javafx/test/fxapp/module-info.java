open module fxapp {
  requires java.base;
  requires java.logging;
  requires transitive javafx.base;
  requires transitive javafx.controls;
  requires transitive javafx.graphics;
  requires org.apiguardian.api;
  requires org.junit.jupiter.api;

  exports fxapp;
}
