package test.integration.core;

import java.util.Map;
import test.base.resource.WebServer;

class VolatileServer extends WebServer {

  @Override
  protected Map<String, Asset> createAssets() {
    return Map.of(
        "/", Asset.ofText("<root>"),
        "/index.html", Asset.ofText("Hello World!"),
        "/123.bytes", Asset.of(new byte[] {1, 2, 3}),
        "/456.bytes", Asset.of(new byte[] {4, 5, 6}),
        "/789.bytes", Asset.of(new byte[] {7, 8, 9}));
  }
}
