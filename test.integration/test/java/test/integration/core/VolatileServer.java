package test.integration.core;

import java.util.Base64;
import java.util.Map;
import test.base.resource.WebServer;

class VolatileServer extends WebServer {

  @Override
  protected Map<String, Asset> createAssets() {
    return Map.of(
        "/", Asset.ofText("<root>"),
        //
        "/index.html", Asset.ofText("Hello World!"),
        //
        "/123.bytes", Asset.of(new byte[] {1, 2, 3}),
        "/456.bytes", Asset.of(new byte[] {4, 5, 6}),
        "/789.bytes", Asset.of(new byte[] {7, 8, 9}),
        //
        "/bar.jar", Asset.of(Base64.getDecoder().decode(BAR_JAR_B64)),
        "/foo.jar", Asset.of(Base64.getDecoder().decode(FOO_JAR_B64))
        //
        );
  }

  /**
   * <pre>
   * echo "module foo {}" > foo/module-info.java
   * javac --release 9 --module foo --module-source-path . -d classes
   * jar --create --file foo.jar -C classes/foo .
   * base64 foo.jar > FOO_JAR_B64
   * </pre>
   */
  static final String FOO_JAR_B64 =
      """
      UEsDBBQACAgIAFtGm1IAAAAAAAAAAAAAAAAJAAQATUVUQS1JTkYv/soAAAMAUEsHCAAAAAACAAAA\
      AAAAAFBLAwQUAAgICABbRptSAAAAAAAAAAAAAAAAFAAAAE1FVEEtSU5GL01BTklGRVNULk1G803M\
      y0xLLS7RDUstKs7Mz7NSMNQz4OVyLkpNLElN0XWqBAqY6RnoGSpo+BclJuekKjjnFxXkFyWWABVr\
      8nLxcgEAUEsHCOSfHd5CAAAAQgAAAFBLAwQUAAgICABbRptSAAAAAAAAAAAAAAAAEQAAAG1vZHVs\
      ZS1pbmZvLmNsYXNzO/Vv1z4GBgYbBm5GBu7c/JTSnFTdzLy0fHYGRkYGASQBvazEskRGBua0/Hxh\
      BhZGBk4QXy8psThVmIGNkYHN0EzPQM+QkYErOL+0KDnVLTMnFSjqCzaggYGBiQEGmBg4wSQzAxeQ\
      FmNgBYsyMrADVXHAFAEAUEsHCK+edbB1AAAAlwAAAFBLAQIUABQACAgIAFtGm1IAAAAAAgAAAAAA\
      AAAJAAQAAAAAAAAAAAAAAAAAAABNRVRBLUlORi/+ygAAUEsBAhQAFAAICAgAW0abUuSfHd5CAAAA\
      QgAAABQAAAAAAAAAAAAAAAAAPQAAAE1FVEEtSU5GL01BTklGRVNULk1GUEsBAhQAFAAICAgAW0ab\
      Uq+edbB1AAAAlwAAABEAAAAAAAAAAAAAAAAAwQAAAG1vZHVsZS1pbmZvLmNsYXNzUEsFBgAAAAAD\
      AAMAvAAAAHUBAAAAAA==""";

  /**
   * <pre>
   * echo "module bar { requires foo; }" > bar/module-info.java
   * javac --release 9 --module bar --module-source-path . -d classes
   * jar --create --file bar.jar -C classes/bar .
   * base64 bar.jar > BAR_JAR_B64.txt
   * </pre>
   */
  static final String BAR_JAR_B64 =
      """
      UEsDBBQACAgIAI9Lm1IAAAAAAAAAAAAAAAAJAAQATUVUQS1JTkYv/soAAAMAUEsHCAAAAAACAAAA\
      AAAAAFBLAwQUAAgICACPS5tSAAAAAAAAAAAAAAAAFAAAAE1FVEEtSU5GL01BTklGRVNULk1G803M\
      y0xLLS7RDUstKs7Mz7NSMNQz4OVyLkpNLElN0XWqBAqY66YmKmj4FyUm56QqOOcXFeQXJZYA1Wry\
      cvFyAQBQSwcILtorFEEAAABBAAAAUEsDBBQACAgIAI9Lm1IAAAAAAAAAAAAAAAARAAAAbW9kdWxl\
      LWluZm8uY2xhc3M79W/XPgYGBlMGHkYG7tz8lNKcVN3MvLR8dgZGRgYBJAG9rMSyREYG5qTEImEG\
      FkYGThBfLymxOFWYgQ0onpafL8zAwcjAFZxfWpSc6paZk8rIwOYLNqCBgYGJAQaYGLjAJDMDN5CW\
      YWCFirI3gGhOBiQAAFBLBwjlFKkddAAAAJ0AAABQSwECFAAUAAgICACPS5tSAAAAAAIAAAAAAAAA\
      CQAEAAAAAAAAAAAAAAAAAAAATUVUQS1JTkYv/soAAFBLAQIUABQACAgIAI9Lm1Iu2isUQQAAAEEA\
      AAAUAAAAAAAAAAAAAAAAAD0AAABNRVRBLUlORi9NQU5JRkVTVC5NRlBLAQIUABQACAgIAI9Lm1Ll\
      FKkddAAAAJ0AAAARAAAAAAAAAAAAAAAAAMAAAABtb2R1bGUtaW5mby5jbGFzc1BLBQYAAAAAAwAD\
      ALwAAABzAQAAAAA=""";
}
