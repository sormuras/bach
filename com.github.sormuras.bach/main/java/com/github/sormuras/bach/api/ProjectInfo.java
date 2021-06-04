package com.github.sormuras.bach.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.MODULE)
public @interface ProjectInfo {

  String BACH_ROOT = ".";
  String BACH_FOLDER = ".bach";
  String BACH_INFO_MODULE_NAME = "bach.info";

  String DEFAULT_NAME = ".";
  String DEFAULT_VERSION = "0";

  int DEFAULT_MAIN_JAVA_RELEASE = 0;

  String FOLDER_EXTERNAL_MODULES = ".bach/external-modules";
  String FOLDER_EXTERNAL_TOOLS = ".bach/external-tools";
  String FOLDER_WORKSPACE = ".bach/workspace";
  String FOLDER_MAIN_MODULES = ".bach/workspace/modules";

  String[] PATTERN_MAIN_MODULES = {"module-info.java", "*", "**"};
  String[] PATTERN_TEST_MODULES = {"test", "**/test", "**/test/**"};

  String[] arguments() default {};

  String name() default DEFAULT_NAME;

  String version() default DEFAULT_VERSION;

  String[] requires() default {};

  External external() default @External;

  Main main() default @Main;

  Test test() default @Test;

  Tool tool() default @Tool;

  // ---

  @Target({})
  @interface External {
    ExternalModule[] modules() default {};

    ExternalLibrary[] libraries() default {};
  }

  @Target({})
  @interface ExternalModule {
    String name();

    String link();

    LinkType type() default LinkType.AUTO;

    String mavenRepository() default "https://repo.maven.apache.org/maven2";
  }

  @Target({})
  @interface ExternalLibrary {
    ExternalLibraryName name();

    String version();
  }

  @Target({})
  @interface Main {

    int javaRelease() default DEFAULT_MAIN_JAVA_RELEASE;

    String[] modulesPatterns() default {"module-info.java", "*", "**"};

    String[] modulePaths() default {FOLDER_EXTERNAL_MODULES};

    boolean jarWithSources() default false;
  }

  @Target({})
  @interface Test {

    String[] modulesPatterns() default {"test", "**/test", "**/test/**"};

    String[] modulePaths() default {FOLDER_MAIN_MODULES, FOLDER_EXTERNAL_MODULES};
  }

  @Target({})
  @interface Tool {
    String[] limit() default {};
    String[] skip() default {};
    Tweak[] tweaks() default {};
  }

  @Target({})
  @interface Tweak {
    String tool();

    String with();

    String[] more() default {};

    CodeSpace[] spaces() default {CodeSpace.MAIN, CodeSpace.TEST};
  }
}
