package de.sormuras.bach;

import java.net.URI;

/*BODY*/
/** Unchecked exception thrown when a module name is not mapped. */
public /*STATIC*/ class UnmappedModuleException extends IllegalStateException {

  public static String throwForString(String module) {
    throw new UnmappedModuleException(module);
  }

  public static URI throwForURI(String module) {
    throw new UnmappedModuleException(module);
  }

  private static final long serialVersionUID = 6985648789039587477L;

  public UnmappedModuleException(String module) {
    super("Module " + module + " is not mapped");
  }
}
