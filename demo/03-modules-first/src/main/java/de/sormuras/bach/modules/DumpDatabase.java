package de.sormuras.bach.modules;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

public class DumpDatabase {

  public static void main(String[] args) {
    createMap().entrySet().forEach(System.out::println);
  }

  private static Map<String, String> createMap() {
    var map = new TreeMap<String, String>();
    var csv = readSpreadsheet("csv");
    for (var row : csv.split("\\R")) {
      var data = row.replace('"', ' ').split(",");
      var module = data[0].trim();
      var group = data[1].trim();
      var artifact = data[2].trim();
      map.put(module, group + ':' + artifact);
    }
    return map;
  }

  private static String readSpreadsheet(String out, String... options) {
    var builder = new StringBuilder("https://docs.google.com/spreadsheets/d");
    builder.append("/1Cl3D8T3SarwxvB_nC_1n1cSl-pTF3XMjBuB7U3jwjjw");
    builder.append("/gviz/tq");
    builder.append("?tqx=out:").append(out);
    builder.append("&sheet=0");
    builder.append("&range=A:C");
    for (var option : options) {
      builder.append("&").append(option);
    }
    try (var input = URI.create(builder.toString()).toURL().openStream();
        var output = new ByteArrayOutputStream()) {
      input.transferTo(output);
      return output.toString("UTF-8");
    } catch (Exception exception) {
      throw new RuntimeException("", exception);
    }
  }
}
