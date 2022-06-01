import static java.util.Comparator.comparing;

import java.security.Provider;
import java.security.Security;
import java.util.stream.Stream;

class security {
  public static void main(String... args) {
    listSecurityProviders();
  }

  static void listSecurityProviders() {
    var providers = Stream.of(Security.getProviders()).sorted(comparing(Provider::getName));
    for (var provider : providers.toList()) {
      System.out.println(provider);
      System.out.println("  name = " + provider.getName());
      System.out.println("  info = " + provider.getInfo());
      var services = provider.getServices().stream().sorted(comparing(Provider.Service::getType));
      for (var service : services.toList()) {
        System.out.println("    service = " + service.getType() + " -> " + service.getAlgorithm());
      }
    }
  }
}
