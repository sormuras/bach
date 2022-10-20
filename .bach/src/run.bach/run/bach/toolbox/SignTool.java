package run.bach.toolbox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import run.bach.Bach;
import run.bach.ToolOperator;
import run.bach.internal.PathSupport;

public record SignTool(String name) implements ToolOperator {

  public SignTool() {
    this("sign");
  }

  @Override
  public void operate(Bach bach, List<String> arguments) throws Exception {
    assert arguments.get(0).equals("--verify");
    assert arguments.get(1).equals("--file");
    var file = Path.of(arguments.get(2));
    verify(bach, file, bach.cli().trustSignatureEmails());
  }

  void verify(Bach bach, Path file, List<String> emails) throws Exception {
    var client = bach.browser().client();
    var hash = PathSupport.checksum(file, "SHA-256");
    var hashResult =
        search(
            client,
            """
            {
              "hash": "sha256:%s"
            }
            """
                .formatted(hash));
    if (hashResult.equals("[]")) {
      throw new RuntimeException("No entry found for https://rekor.tlog.dev/?hash=" + hash);
    }
    if (emails.isEmpty()) {
      bach.info("No trusted emails configured for https://rekor.tlog.dev/?hash=" + hash);
      return;
    }
    for (var email : emails) {
      var result =
          search(
              client,
              """
              {
                "email": "%s",
                "hash": "sha256:%s",
                "operator": "and"
              }
              """
                  .formatted(email, hash));
      if (result.equals("[]")) continue;
      bach.debug("Found trusted entry: " + result);
      return;
    }
    throw new RuntimeException("No trusted entry found for https://rekor.tlog.dev/?hash=" + hash);
  }

  static String search(HttpClient client, String query) throws Exception {
    var request =
        HttpRequest.newBuilder()
            .uri(new URI("https://rekor.sigstore.dev" + "/api/v1/index/retrieve"))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .method("POST", HttpRequest.BodyPublishers.ofString(query))
            .build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 400) return response.body().strip();
    throw new Exception("Bad response status: " + response + "\n" + response.body());
  }
}
