package test.base;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.nio.file.Path;

public class Classes {

  public static int feature(Path path) {
    return readMajorVersionNumber(path) - 44;
  }

  public static int readMajorVersionNumber(Path path) {
    try (var data = new DataInputStream(new FileInputStream(path.toFile()))) {
      data.skipBytes(4); // var magicNumber = data.readInt(); 0xCAFEBABE
      data.skipBytes(2); // var minorVersion = data.readUnsignedShort();
      return data.readUnsignedShort();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private Classes() {}
}
