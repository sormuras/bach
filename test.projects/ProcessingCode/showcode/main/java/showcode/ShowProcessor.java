package showcode;

import com.sun.source.util.DocTrees;
import java.io.PrintWriter;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("*")
public class ShowProcessor extends AbstractProcessor {
  PrintWriter out;
  DocTrees treeUtils;

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public void init(ProcessingEnvironment pEnv) {
    out = new PrintWriter(System.out);
    out.printf("#%n# ShowProcessor.init%n#%n");
    treeUtils = DocTrees.instance(pEnv);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    new ShowCode(treeUtils).show(roundEnv.getRootElements(), out);
    out.flush();
    return false;
  }
}
