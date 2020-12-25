/**
 * Defines the API of 🎼 Bach, the Java Shell Builder.
 *
 * <h2>Modules</h2>
 *
 * A module is either declared by the current project or it is a non-project module. A non-project
 * module is either a system module provided by the Java Runtime or it is an external module that is
 * acquirable and stored in a local directory.
 *
 * <h3>Declared Modules</h3>
 *
 * A declared module is defined via a {@code module-info.java} file located within the current
 * project's file tree. A declared module is either a main module or a test module. By convention,
 * the source code for modules is in a directory that is the name of the module. Here's an example
 * project declaring two main modules ({@code com.greetings}, {@code org.astro}) and one test module
 * ({@code test.integration}).
 *
 * <pre>{@code
 * com.greetings
 *   main
 *     java
 *       com
 *         greetings
 *           Main.java
 *       module-info.java
 * org.astro
 *   main
 *     java
 *       org
 *         astro
 *           World.java
 *       module-info.java
 * test.integration
 *   test
 *     java
 *       test
 *         integration
 *           AstroTests.java
 *           GreetingsTests.java
 *       module-info.java
 * }</pre>
 *
 * <h3>System Modules</h3>
 *
 * A system module is provided by a Java Runtime. Here's an excerpt of a listing of system modules
 * provided by a standard OpenJDK Runtime image.
 *
 * <pre>{@code
 * java.base
 * java.compiler
 * java.datatransfer
 * java.desktop
 * java.instrument
 * java.logging
 * ...
 * jdk.zipfs
 * }</pre>
 *
 * Consult the output of the "{@code java --list-modules}" command for a complete listing of all
 * observable system modules for a specific Java image. Also see {@link
 * java.lang.module.ModuleFinder#ofSystem() ModuleFinder.ofSystem()} for details.
 *
 * <h3>External Modules</h3>
 *
 * An external module is not declared by the current project nor is it provided by the Java Runtime.
 * An external module has to be provided by the user in form of a modular JAR file.
 *
 * <p>For example, {@code org.junit.jupiter} and all other modules published by the JUnit-Team are
 * considered external modules. Here's a listing of external modules used to compile and launch
 * JUnit Jupiter test runs.
 *
 * <pre>{@code
 * org.apiguardian.api.jar
 * org.junit.jupiter.api.jar
 * org.junit.jupiter.engine.jar
 * org.junit.jupiter.jar
 * org.junit.jupiter.params.jar
 * org.junit.platform.commons.jar
 * org.junit.platform.console.jar
 * org.junit.platform.engine.jar
 * org.junit.platform.launcher.jar
 * org.junit.platform.reporting.jar
 * org.opentest4j.jar
 * }</pre>
 *
 * By default, the directory {@link com.github.sormuras.bach.project.ProjectInfo#EXTERNAL_MODULES
 * .bach/external-modules} contains the user-provided modular JAR files.
 *
 * <h2>Links</h2>
 *
 * <ul>
 *   <li>Bach's <a href="https://github.com/sormuras/bach">Code &amp; Issues</a>
 *   <li>Bach's <a href="https://sormuras.github.io/bach">User Guide</a>
 *   <li>Java® Development Kit Version 15 <a
 *       href="https://docs.oracle.com/en/java/javase/15/docs/specs/man/">Tool Specifications</a>
 * </ul>
 *
 * @uses java.util.spi.ToolProvider
 */
@com.github.sormuras.bach.project.ProjectInfo
module com.github.sormuras.bach {
  exports com.github.sormuras.bach;
  exports com.github.sormuras.bach.project;
  exports com.github.sormuras.bach.tool;

  requires transitive java.net.http;
  requires jdk.compiler;
  requires jdk.crypto.ec; // https://stackoverflow.com/questions/55439599
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jlink;

  uses com.github.sormuras.bach.BuildProgram;
  uses java.util.spi.ToolProvider;

  provides java.util.spi.ToolProvider with
      com.github.sormuras.bach.internal.BachToolProvider;
}
