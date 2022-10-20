package run.bach.toolfinder;

import java.lang.module.FindException;
import java.util.List;
import java.util.function.Supplier;
import run.bach.Tool;
import run.bach.ToolFinder;

public record SupplierToolFinder(String description, Supplier<ToolFinder> supplier)
    implements ToolFinder {
  @Override
  public List<Tool> findAll() {
    try {
      return supplier.get().findAll();
    } catch (FindException ignore) {
      return List.of();
    }
  }
}
