package run.bach;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;

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

  Space[] spaces() default {};

  @Target({})
  @interface Space {
    String name();

    String[] requires() default {};

    int release() default 0;

    String[] launchers() default {};

    Module[] modules() default {};
  }

  @Target({})
  @interface Module {
    String content();

    String info();
  }

  record Support(ProjectInfo info) {
    public static Support of(java.lang.Module module) {
      var element = module.isAnnotationPresent(ProjectInfo.class) ? module : Bach.class.getModule();
      var info = element.getAnnotation(ProjectInfo.class);
      if (info == null) throw new AssertionError();
      return new Support(info);
    }

    public Project build() {
      return new Project(new Project.Name(name()), new Project.Version(info.version()));
    }

    public String name() {
      var name = info.name();
      return name.equals("*") ? Path.of("").toAbsolutePath().getFileName().toString() : name;
    }
  }
}
