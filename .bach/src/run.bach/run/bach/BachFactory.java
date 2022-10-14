package run.bach;

import java.util.ServiceLoader;

@FunctionalInterface
public interface BachFactory {
  Bach createBach(Configuration configuration);

  static Bach newBach(Configuration configuration) {
    var factory = ServiceLoader.load(BachFactory.class).findFirst().orElse(Bach::new);
    var bach = factory.createBach(configuration);
    bach.debug("Initialized instance of " + bach.getClass());
    bach.debug(bach.toString(0));
    return bach;
  }
}
