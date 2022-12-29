@org.junit.platform.commons.annotation.Testable
open /* for testing */ module test.junit {
  requires org.junit.jupiter; // for writing tests
  requires org.junitpioneer; // for writing more tests
  requires static org.junit.platform.console; // for running tests
}
