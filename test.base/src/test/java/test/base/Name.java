package test.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Name {
  String value();

  record Support(Class<?> type) {
    public String name() {
      return type.getDeclaredAnnotation(Name.class).value();
    }

    public boolean test(String name) {
      var annotation = type.getDeclaredAnnotation(Name.class);
      return annotation != null && name.equals(annotation.value());
    }
  }
}
