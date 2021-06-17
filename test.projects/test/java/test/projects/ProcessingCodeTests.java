package test.projects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class ProcessingCodeTests {

  @Test
  void build() throws Exception {
    var project = TestProject.of("ProcessingCode");
    assertEquals(0, project.build().waitFor());

    assertLinesMatch(
        """
        >>>>
        #
        # ShowProcessor.process
        #
        | CLASS tests.Tests
          | CONSTRUCTOR Tests()
        | MODULE tests
          # DOC_COMMENT Defines the API of the tests module.
            # TEXT Defines the API of the tests module.
          | PACKAGE tests
            | CLASS tests.Tests
              | CONSTRUCTOR Tests()
        #
        # ShowProcessor.process
        #
        #
        # ShowPlugin.finished
        #
        | CLASS <anonymous >
        #
        # ShowPlugin.finished
        #
        | CLASS tests.Tests
          | CONSTRUCTOR Tests()
        >>>>
        | MODULE showcode
          # DOC_COMMENT Defines classes and interfaces of the ShowCode API.
            # TEXT Defines classes and interfaces of the ShowCode API.
          | PACKAGE showcode
            | CLASS showcode.ShowCode
              # DOC_COMMENT A class to display the structure of a series of elements and their documentation comments.
                # TEXT A class to display the structure of a series of elements and their documentation comments.
              | CLASS showcode.ShowCode.ShowElements
                # DOC_COMMENT A scanner to display the structure of a series of elements and their documentation comments.
                  # TEXT A scanner to display the structure of a series of elements and their documentation comments.
                | FIELD out
                | CONSTRUCTOR ShowElements(java.io.PrintWriter)
                  | PARAMETER out
                | METHOD show(java.util.Set<? extends javax.lang.model.element.Element>)
                  | PARAMETER elements
                | METHOD scan(javax.lang.model.element.Element,java.lang.Integer)
                  | PARAMETER e
                  | PARAMETER depth
              | CLASS showcode.ShowCode.ShowDocTrees
                # DOC_COMMENT A scanner to display the structure of a documentation comment.
                  # TEXT A scanner to display the structure of a documentation comment.
                | FIELD out
                | CONSTRUCTOR ShowDocTrees(java.io.PrintWriter)
                  | PARAMETER out
                | METHOD scan(com.sun.source.doctree.DocTree,java.lang.Integer)
                  | PARAMETER t
                  | PARAMETER depth
              | FIELD treeUtils
              | CONSTRUCTOR ShowCode(com.sun.source.util.DocTrees)
                | PARAMETER treeUtils
              | METHOD show(java.util.Set<? extends javax.lang.model.element.Element>,java.io.PrintWriter)
                | PARAMETER elements
                | PARAMETER out
            | CLASS showcode.ShowDoclet
              # DOC_COMMENT A simple doclet to demonstrate the use of various APIs.
              #    @see jdk.javadoc.doclet.Doclet
              #    @version 1.0
                # TEXT A simple doclet to demonstrate the use of various APIs.
                # SEE @see jdk.javadoc.doclet.Doclet
                  # REFERENCE jdk.javadoc.doclet.Doclet
                # VERSION @version 1.0
                  # TEXT 1.0
              | CONSTRUCTOR ShowDoclet()
              | METHOD init(java.util.Locale,jdk.javadoc.doclet.Reporter)
                | PARAMETER locale
                | PARAMETER reporter
              | METHOD getName()
              | METHOD getSupportedOptions()
              | METHOD getSupportedSourceVersion()
              | METHOD run(jdk.javadoc.doclet.DocletEnvironment)
                | PARAMETER environment
            | CLASS showcode.ShowPlugin
              | CONSTRUCTOR ShowPlugin()
              | FIELD treeUtils
              | FIELD out
              | METHOD getName()
              | METHOD init(com.sun.source.util.JavacTask,java.lang.String...)
                | PARAMETER task
                | PARAMETER args
              | METHOD finished(com.sun.source.util.TaskEvent)
                | PARAMETER e
            | CLASS showcode.ShowProcessor
              | CONSTRUCTOR ShowProcessor()
              | FIELD out
              | FIELD treeUtils
              | METHOD init(javax.annotation.processing.ProcessingEnvironment)
                | PARAMETER pEnv
              | METHOD process(java.util.Set<? extends javax.lang.model.element.TypeElement>,javax.annotation.processing.RoundEnvironment)
                | PARAMETER annotations
                | PARAMETER roundEnv
        """
            .lines(),
        Files.readString(project.folders().root("build-out.log")).lines());
  }
}
