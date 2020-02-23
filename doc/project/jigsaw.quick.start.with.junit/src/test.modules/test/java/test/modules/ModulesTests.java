package test.modules;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import test.base.PrependModuleName;

@DisplayNameGeneration(PrependModuleName.class)
class ModulesTests {
  @Test
  void test() {}
}
