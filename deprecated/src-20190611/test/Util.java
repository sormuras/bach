import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.junit.jupiter.api.condition.OS;

interface Util {

  /** Platform-agnostic {@code chmod} implementation. */
  static void chmod(Path path, boolean r, boolean w, boolean x) throws Exception {
    if (OS.WINDOWS.isCurrentOs()) {
      var upls = path.getFileSystem().getUserPrincipalLookupService();
      var user = upls.lookupPrincipalByName(System.getProperty("user.name"));
      var builder = AclEntry.newBuilder();
      var permissions =
          EnumSet.of(
              // AclEntryPermission.EXECUTE, // "x"
              // AclEntryPermission.READ_DATA, // "r"
              AclEntryPermission.READ_ATTRIBUTES,
              AclEntryPermission.READ_NAMED_ATTRS,
              // AclEntryPermission.WRITE_DATA, // "w"
              // AclEntryPermission.APPEND_DATA, // "w"
              AclEntryPermission.WRITE_ATTRIBUTES,
              AclEntryPermission.WRITE_NAMED_ATTRS,
              AclEntryPermission.DELETE_CHILD,
              AclEntryPermission.DELETE,
              AclEntryPermission.READ_ACL,
              AclEntryPermission.WRITE_ACL,
              AclEntryPermission.WRITE_OWNER,
              AclEntryPermission.SYNCHRONIZE);
      if (r) {
        permissions.add(AclEntryPermission.READ_DATA); // == LIST_DIRECTORY
      }
      if (w) {
        permissions.add(AclEntryPermission.WRITE_DATA); // == ADD_FILE
        permissions.add(AclEntryPermission.APPEND_DATA); // == ADD_SUBDIRECTORY
      }
      if (x) {
        permissions.add(AclEntryPermission.EXECUTE);
      }
      builder.setPermissions(permissions);
      builder.setPrincipal(user);
      builder.setType(AclEntryType.ALLOW);
      var aclAttr = Files.getFileAttributeView(path, AclFileAttributeView.class);
      aclAttr.setAcl(List.of(builder.build()));
      return;
    }
    var user = (r ? "r" : "-") + (w ? "w" : "-") + (x ? "x" : "-");
    Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(user + "------"));
  }

  /** Create {@code count} regular files in the supplied directory. */
  static List<Path> createFiles(Path directory, int count) throws Exception {
    var paths = new ArrayList<Path>();
    for (int i = 0; i < count; i++) {
      paths.add(Files.createFile(directory.resolve("file-" + i)));
    }
    return paths;
  }

  /** Walk directory tree structure. */
  static List<String> treeWalk(Path root) {
    var lines = new ArrayList<String>();
    treeWalk(root, lines::add);
    return lines;
  }

  /** Walk directory tree structure. */
  static void treeWalk(Path root, Consumer<String> out) {
    try (var stream = Files.walk(root)) {
      stream
          .map(root::relativize)
          .map(path -> path.toString().replace('\\', '/'))
          .sorted()
          .filter(Predicate.not(String::isEmpty))
          .forEach(out);
    } catch (Exception e) {
      throw new Error("Walking tree failed: " + root, e);
    }
  }
}
