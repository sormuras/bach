package test.modules;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

public class JUnit4Test {
  @Test
  public void test() {
    MatcherAssert.assertThat(Log.ofNullWriter(), Matchers.notNullValue());
  }
}
