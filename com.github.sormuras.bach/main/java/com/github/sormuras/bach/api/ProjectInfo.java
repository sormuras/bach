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

  String DEFAULT_PROJECT_NAME = "noname";

  String version() default DEFAULT_PROJECT_VERSION;

  String DEFAULT_PROJECT_VERSION = "0";

  String[] arguments() default {};

  Options options() default @Options;

  String[] requires() default {};

  External external() default @External;

  Main main() default @Main;

  Test test() default @Test;

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

    int javaRelease() default DEFAULT_JAVA_RELEASE;

    int DEFAULT_JAVA_RELEASE = 0;

    String modulesPatterns() default DEFAULT_MODULES_PATTERNS;

    String DEFAULT_MODULES_PATTERNS =
        """
        module-info.java
        *
        **
        """;

    String modulePaths() default DEFAULT_MODULE_PATHS;

    String DEFAULT_MODULE_PATHS = ".bach/external-modules";

    boolean jarWithSources() default false;
  }

  @Target({})
  @interface Test {

    String modulesPatterns() default DEFAULT_MODULES_PATTERNS;

    String DEFAULT_MODULES_PATTERNS =
        """
        test
        **/test
        **/test/**
        """;

    String modulePaths() default DEFAULT_MODULE_PATHS;

    String DEFAULT_MODULE_PATHS =
        """
        .bach/workspace/modules
        """ + Main.DEFAULT_MODULE_PATHS;
  }
}
