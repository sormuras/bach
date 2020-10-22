open /*test*/ module com.github.sormuras.bach {
  exports com.github.sormuras.bach;

  requires jdk.compiler;
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jlink;
  requires org.junit.jupiter;

  uses java.util.spi.ToolProvider;
}
