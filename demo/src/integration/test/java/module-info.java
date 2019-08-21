// --main-class integration.Main
open /*test*/ module integration {
  // modules under test
  requires de.sormuras.bach.demo;
  requires de.sormuras.bach.demo.multi;
  // /*test*/ compile
  requires org.junit.jupiter.api /*5.5.1*/;
  // /*test*/ runtime
  requires org.junit.platform.launcher /*1.5.1*/;
  requires org.junit.jupiter.engine /*5.5.1*/;
}
