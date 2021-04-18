package test.base.magnificat.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.MODULE)
public @interface ProjectInfo {

  /**
   * {@return the name of the project}
   *
   * @see Option#PROJECT_NAME
   */
  String name() default DEFAULT_PROJECT_NAME;

  /**
   * {@return the version of the project}
   *
   * @see Option#PROJECT_VERSION
   */
  String version() default DEFAULT_PROJECT_VERSION;

  String[] arguments() default {};

  Options options() default @Options;

  // ---

  String DEFAULT_PROJECT_NAME = "noname";
  String DEFAULT_PROJECT_VERSION = "0";
  String[] DEFAULT_ACTIONS = {"build"};

  // ---

  @Target({})
  @interface Options {
    /** {@return preset flag-style options} */
    Option[] flags() default {};

    /** {@return preset key-value pair options} */
    Property[] properties() default {};

    /** {@return preset actions} */
    Action[] actions() default {};
  }

  @Target({})
  @interface Property {
    Option option();

    String[] value();
  }
}
