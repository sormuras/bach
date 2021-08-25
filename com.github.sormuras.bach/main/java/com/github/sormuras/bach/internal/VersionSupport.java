package com.github.sormuras.bach.internal;

import java.lang.module.ModuleDescriptor.Version;

/** Static utility methods for operating on instances of {@link Version}. */
public sealed interface VersionSupport permits ConstantInterface {

  record Components(Version version, int numberTerminator, int preReleaseTerminator) {
    public String toNumber() {
      if (numberTerminator == 0) return version.toString();
      return version.toString().substring(0, numberTerminator);
    }
    public String toNumberAndPreRelease() {
      if (preReleaseTerminator == 0) return version.toString();
      return version.toString().substring(0, preReleaseTerminator);
    }
  }

  static Components components(Version version) {
    var string = version.toString();
    var preIndex = 0;
    var buildIndex = 0;
    for (int index = 1; index < string.length(); index++) {
      var c = string.charAt(index);
      if (preIndex == 0 && c == '+' || c == '-') {
        preIndex = index;
        continue;
      }
      if (c == '+') {
        buildIndex = index;
        break;
      }
    }
    return new Components(version, preIndex, buildIndex);
  }

  static String toNumberAndPreRelease(Version version) {
    return components(version).toNumberAndPreRelease();
  }
}
