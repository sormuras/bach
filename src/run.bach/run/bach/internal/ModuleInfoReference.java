package run.bach.internal;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.nio.file.Path;

public class ModuleInfoReference extends ModuleReference {

  private final Path info;

  public ModuleInfoReference(Path info) {
    this(info, ModuleDescriptorSupport.parse(info));
  }

  public ModuleInfoReference(Path info, ModuleDescriptor descriptor) {
    super(descriptor, info.toUri());
    this.info = info;
  }

  @Override
  public boolean equals(Object object) {
    return this == object || object instanceof ModuleInfoReference ref && info.equals(ref.info);
  }

  @Override
  public int hashCode() {
    return info.hashCode();
  }

  public Path info() {
    return info;
  }

  public String name() {
    return descriptor().name();
  }

  @Override
  public ModuleReader open() {
    return new NullModuleReader();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[info=" + info + ']';
  }
}
