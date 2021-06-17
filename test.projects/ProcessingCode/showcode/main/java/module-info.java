/** Defines classes and interfaces of the ShowCode API. */
module showcode {
  requires java.compiler;
  requires jdk.javadoc;

  provides com.sun.source.util.Plugin with
      showcode.ShowPlugin;
  provides javax.annotation.processing.Processor with
      showcode.ShowProcessor;
}
