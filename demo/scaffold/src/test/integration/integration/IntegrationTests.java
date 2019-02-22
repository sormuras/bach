package integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class IntegrationTests {
  @Test
  void test(TestInfo info) {
    System.out.println(info);
  }
}
