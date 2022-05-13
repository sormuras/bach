package processor;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import java.io.PrintWriter;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.util.ElementScanner9;

/** A class to display the structure of a series of elements and their documentation comments. */
public class ShowCode {
  final DocTrees treeUtils;

  public ShowCode(DocTrees treeUtils) {
    this.treeUtils = treeUtils;
  }

  public void show(Set<? extends Element> elements, PrintWriter out) {
    new ShowElements(out).show(elements);
  }

  /**
   * A scanner to display the structure of a series of elements and their documentation comments.
   */
  class ShowElements extends ElementScanner9<Void, Integer> {
    final PrintWriter out;

    ShowElements(PrintWriter out) {
      this.out = out;
    }

    void show(Set<? extends Element> elements) {
      scan(elements, 0);
    }

    @Override
    public Void scan(Element e, Integer depth) {
      String indent = "  ".repeat(depth);
      out.println(indent + "| " + e.getKind() + " " + e);
      DocCommentTree dcTree = treeUtils.getDocCommentTree(e);
      if (dcTree != null) {
        new ShowDocTrees(out).scan(dcTree, depth + 1);
      }
      return super.scan(e, depth + 1);
    }
  }

  /** A scanner to display the structure of a documentation comment. */
  class ShowDocTrees extends DocTreeScanner<Void, Integer> {
    final PrintWriter out;

    ShowDocTrees(PrintWriter out) {
      this.out = out;
    }

    @Override
    public Void scan(DocTree t, Integer depth) {
      String indent = "  ".repeat(depth);
      out.println(
          indent + "# " + t.getKind() + " " + t.toString().replace("\n", "\n" + indent + "#    "));
      return super.scan(t, depth + 1);
    }
  }
}
