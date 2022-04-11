import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/** A finder of {@link ServiceLoader services}. */
@FunctionalInterface
public interface ServiceFinder<S> {

  List<S> findAll();

  default String nameOf(S service) {
    return service.getClass().getSimpleName();
  }

  default Optional<S> find(String name) {
    return findAll().stream().filter(service -> nameOf(service).equals(name)).findFirst();
  }

  record CompositeServiceFinder<S, F extends ServiceFinder<S>>(List<F> finders)
      implements ServiceFinder<S> {
    @Override
    public List<S> findAll() {
      return finders.stream().flatMap(finder -> finder.findAll().stream()).toList();
    }
  }

  record DirectServiceFinder<S>(List<S> findAll) implements ServiceFinder<S> {}

  record ServiceLoaderServiceFinder<S>(ServiceLoader<S> loader) implements ServiceFinder<S> {
    @Override
    public List<S> findAll() {
      synchronized (loader) {
        return loader.stream().map(ServiceLoader.Provider::get).toList();
      }
    }
  }

  record ModuleLayerServiceFinder<S>(ModuleLayer layer, ServiceLoader<S> loader)
      implements ServiceFinder<S> {
    @Override
    public List<S> findAll() {
      synchronized (loader) {
        return loader.stream()
            .filter(service -> service.type().getModule().getLayer() == layer)
            .map(ServiceLoader.Provider::get)
            .toList();
      }
    }
  }
}
