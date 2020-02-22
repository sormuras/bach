package test.base;

import org.junit.jupiter.api.DisplayNameGenerator;

public class PrependModuleName extends DisplayNameGenerator.Standard {

  @Override
  public String generateDisplayNameForClass(Class<?> testClass) {
    return testClass.getModule().getName() + "/" + super.generateDisplayNameForClass(testClass);
  }

  @Override
  public String generateDisplayNameForNestedClass(Class<?> nestedClass) {
    return nestedClass.getModule().getName() + "/" + super.generateDisplayNameForNestedClass(nestedClass);
  }
}
