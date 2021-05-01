package test.base.option;

import java.util.List;
import java.util.Optional;

public interface Cli {
  boolean verbose();

  boolean version();

  Optional<String> name();

  List<Thread.State> states();
}
