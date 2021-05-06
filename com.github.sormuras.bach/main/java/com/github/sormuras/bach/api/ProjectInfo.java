package com.github.sormuras.bach.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.MODULE)
public @interface ProjectInfo {

  String[] arguments() default {};

  String name() default ".";

  String version() default "0";

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

    int javaRelease() default 0;

    String[] modulesPatterns() default {"module-info.java", "*", "**"};

    String[] modulePaths() default {".bach/external-modules"};

    boolean jarWithSources() default false;
  }

  @Target({})
  @interface Test {

    String[] modulesPatterns() default {"test", "**/test", "**/test/**"};

    String[] modulePaths() default {".bach/workspace/modules", ".bach/external-modules"};
  }

  @Target({})
  @interface Tool {
    String[] limit() default {};
    String[] skip() default {};
    Tweak[] tweaks() default {};
  }

  @Target({})
  @interface Tweak {
    String trigger();

    String option();

    String[] value() default {};

    CodeSpace[] spaces() default {CodeSpace.MAIN, CodeSpace.TEST};
  }
}
