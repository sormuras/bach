package run.bach;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated module is a project declaration.
 *
 * <p>In other words, it transforms an annotated {@code module-info.java} compilation unit into a
 * logical {@code project-info.java} compilation unit. Typically, that compilation unit's path is:
 * {@code .bach/project/module-info.java}.
 *
 * <p>This annotation declares a set of un-targeted nested annotations intended solely for use as a
 * member type in complex annotation type declarations. They cannot be used to annotate anything
 * directly.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.MODULE)
public @interface ProjectInfo {
  /**
   * @return the name of the project, or {@code "*"} for a generated name
   */
  String name() default "*";

  /**
   * @return the version of the project, defaults to {@code "0-ea"}
   */
  String version() default "0-ea";

  String moduleContentRootPattern() default "src/${module}";

  String moduleContentInfoPattern() default "${space}/java/module-info.java";

  Space[] spaces() default {};

  @Target({})
  @interface Space {
    String name();

    String[] requires() default {};

    int release() default 0;

    String[] launchers() default {};

    String[] modules() default {};
  }
}
