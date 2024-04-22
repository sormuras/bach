package test.junit;

import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

class JUnitPioneerTests {
  @CartesianTest
  void test(@Values(ints = {1, 2}) int x, @Values(ints = {3, 4}) int y) {
    assert x * y > 0;
  }
}
