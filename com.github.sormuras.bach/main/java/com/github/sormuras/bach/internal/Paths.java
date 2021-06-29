package com.github.sormuras.bach.internal;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

/** {@link Path}-related utilities. */
public class Paths {

  /** {@return the message digest of the given file as a hexadecimal string} */
  public static String hash(Path file, String algorithm) throws Exception {
    var md = MessageDigest.getInstance(algorithm);
    try (var in = new BufferedInputStream(new FileInputStream(file.toFile()));
        var out = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
      in.transferTo(out);
    }
    return String.format("%0" + (md.getDigestLength() * 2) + "x", new BigInteger(1, md.digest()));
  }

  /** Hidden default constructor. */
  private Paths() {}
}
