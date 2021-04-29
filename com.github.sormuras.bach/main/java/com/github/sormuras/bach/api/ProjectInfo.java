package com.github.sormuras.bach.api;

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

  String version() default DEFAULT_PROJECT_VERSION;

  String[] arguments() default {};

  Options options() default @Options;

  String[] requires() default {};

  External external() default @External;

  Main main() default @Main;

  // ---

  String DEFAULT_PROJECT_NAME = "noname";
  String DEFAULT_PROJECT_VERSION = "0";

  String[] DEFAULT_MAIN_MODULES_PATTERNS = {"module-info.java", "*", "**"};
  String[] DEFAULT_MAIN_MODULE_PATH = {".bach/external-modules"};

  String[] DEFAULT_TEST_MODULES_PATTERNS = {"test", "**/test", "**/test/**"};
  String[] DEFAULT_TEST_MODULE_PATH = {".bach/workspace/modules",".bach/external-modules"};

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

  @Target({})
  @interface External {
    ExternalModule[] externalModules() default {};
    ExternalLibrary[] externalLibraries() default {};
  }

  @Target({})
  @interface ExternalLibrary {
    ExternalLibraryName name();
    String version();
  }

  @Target({})
  @interface ExternalModule {
    String name();
    String link();
    LinkType type() default LinkType.AUTO;
    String mavenRepository() default "https://repo.maven.apache.org/maven2";
  }

  enum LinkType {
    AUTO,
    URI,
    MAVEN
  }

  @Target({})
  @interface Main {

    int DEFAULT_JAVA_RELEASE = 0;

    int javaRelease() default DEFAULT_JAVA_RELEASE;
  }
}
