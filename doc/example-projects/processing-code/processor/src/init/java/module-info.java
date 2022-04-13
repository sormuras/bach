/** Defines classes and interfaces of the Processor API. */
module processor {
  requires java.compiler;
  requires jdk.compiler;
  requires jdk.javadoc;

  provides com.sun.source.util.Plugin with
      processor.ShowPlugin;
  provides javax.annotation.processing.Processor with
      processor.ShowProcessor;
}
