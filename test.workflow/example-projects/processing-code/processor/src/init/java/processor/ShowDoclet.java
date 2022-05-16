package processor;

import com.sun.source.util.DocTrees;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

/**
 * A simple doclet to demonstrate the use of various APIs.
 *
 * @see jdk.javadoc.doclet.Doclet
 * @version 1.0
 */
public class ShowDoclet implements Doclet {
  @Override
  public void init(Locale locale, Reporter reporter) {}

  @Override
  public String getName() {
    return "showDoclet";
  }

  @Override
  public Set<? extends Option> getSupportedOptions() {
    return Set.of();
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public boolean run(DocletEnvironment environment) {
    DocTrees treeUtils = environment.getDocTrees();
    Set<? extends Element> elements = environment.getSpecifiedElements();
    PrintWriter out = new StringPrintWriter(); // new PrintWriter(System.out);
    out.printf("#%n# ShowDoclet.run%n#%n");

    new ShowCode(treeUtils).show(elements, out);

    out.flush();
    return true;
  }
}
