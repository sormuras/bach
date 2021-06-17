package showcode;

import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import java.io.PrintWriter;
import java.util.Set;

public class ShowPlugin implements Plugin, TaskListener {
  private DocTrees treeUtils;
  private PrintWriter out;

  @Override
  public String getName() {
    return "showPlugin";
  }

  @Override
  public void init(JavacTask task, String... args) {
    out = new PrintWriter(System.out);
    out.printf("#%n# ShowPlugin.init%n#%n");
    treeUtils = DocTrees.instance(task);
    task.addTaskListener(this);
  }

  @Override
  public void finished(TaskEvent e) {
    switch (e.getKind()) {
      case ANALYZE:
        new ShowCode(treeUtils).show(Set.of(e.getTypeElement()), out);
        out.flush();
    }
  }
}
