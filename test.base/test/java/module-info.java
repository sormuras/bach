@test.base.magnificat.api.ProjectInfo
open /*test*/ module test.base {
  exports test.base;
  exports test.base.magnificat;
  exports test.base.magnificat.api;
  exports test.base.magnificat.core;

  requires jdk.xml.dom; // #217
  requires transitive org.junit.jupiter;

  uses test.base.magnificat.Binding;

  provides java.util.spi.ToolProvider with
      test.base.TestProvider1,
      test.base.TestProvider2,
      test.base.magnificat.BachToolProvider;
}
