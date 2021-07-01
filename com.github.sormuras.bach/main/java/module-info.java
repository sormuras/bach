/**
 * Defines the API of 🎼 Bach, the Java Shell Builder.
 *
 * <h2>Links</h2>
 *
 * <ul>
 *   <li>Bach's <a href="https://github.com/sormuras/bach">Code &amp; Issues</a>
 *   <li>Bach's <a href="https://sormuras.github.io/api/#bach">API</a>
 *   <li>Java® Development Kit Version 16 <a
 *       href="https://docs.oracle.com/en/java/javase/16/docs/specs/man/">Tool Specifications</a>
 * </ul>
 *
 * @uses java.util.spi.ToolProvider
 */
module com.github.sormuras.bach {
  exports com.github.sormuras.bach;
  exports com.github.sormuras.bach.call;
  exports com.github.sormuras.bach.project;
  exports com.github.sormuras.bach.settings;
  exports com.github.sormuras.bach.workflow;

  requires java.base;
  requires transitive java.net.http;
  requires jdk.compiler;
  requires jdk.crypto.ec; // https://stackoverflow.com/questions/55439599
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jfr;
  requires jdk.jlink;

  uses java.util.spi.ToolProvider;

  provides com.sun.source.util.Plugin with
      com.github.sormuras.bach.internal.ModuleDescriptors.Parser;
}
