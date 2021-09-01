package test.integration.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.sormuras.bach.external.Jackson;
import java.io.StringReader;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class JacksonTests {
  @Test
  void locate() throws Exception {
    var modules =
        """
        com.fasterxml.jackson.annotation=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.13.0-rc2/jackson-annotations-2.13.0-rc2.jar
        com.fasterxml.jackson.core=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/core/jackson-core/2.13.0-rc2/jackson-core-2.13.0-rc2.jar
        com.fasterxml.jackson.databind=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.13.0-rc2/jackson-databind-2.13.0-rc2.jar
        com.fasterxml.jackson.dataformat.avro=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/dataformat/jackson-dataformat-avro/2.13.0-rc2/jackson-dataformat-avro-2.13.0-rc2.jar
        com.fasterxml.jackson.dataformat.cbor=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/dataformat/jackson-dataformat-cbor/2.13.0-rc2/jackson-dataformat-cbor-2.13.0-rc2.jar
        com.fasterxml.jackson.dataformat.csv=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/dataformat/jackson-dataformat-csv/2.13.0-rc2/jackson-dataformat-csv-2.13.0-rc2.jar
        com.fasterxml.jackson.dataformat.ion=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/dataformat/jackson-dataformat-ion/2.13.0-rc2/jackson-dataformat-ion-2.13.0-rc2.jar
        com.fasterxml.jackson.dataformat.javaprop=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/dataformat/jackson-dataformat-properties/2.13.0-rc2/jackson-dataformat-properties-2.13.0-rc2.jar
        com.fasterxml.jackson.dataformat.protobuf=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/dataformat/jackson-dataformat-protobuf/2.13.0-rc2/jackson-dataformat-protobuf-2.13.0-rc2.jar
        com.fasterxml.jackson.dataformat.smile=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/dataformat/jackson-dataformat-smile/2.13.0-rc2/jackson-dataformat-smile-2.13.0-rc2.jar
        com.fasterxml.jackson.dataformat.toml=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/dataformat/jackson-dataformat-toml/2.13.0-rc2/jackson-dataformat-toml-2.13.0-rc2.jar
        com.fasterxml.jackson.dataformat.xml=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/dataformat/jackson-dataformat-xml/2.13.0-rc2/jackson-dataformat-xml-2.13.0-rc2.jar
        com.fasterxml.jackson.dataformat.yaml=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.13.0-rc2/jackson-dataformat-yaml-2.13.0-rc2.jar
        com.fasterxml.jackson.datatype.guava=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-guava/2.13.0-rc2/jackson-datatype-guava-2.13.0-rc2.jar
        com.fasterxml.jackson.datatype.hppc=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-hppc/2.13.0-rc2/jackson-datatype-hppc-2.13.0-rc2.jar
        com.fasterxml.jackson.datatype.jakarta.mail=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-jakarta-mail/2.13.0-rc2/jackson-datatype-jakarta-mail-2.13.0-rc2.jar
        com.fasterxml.jackson.datatype.jaxrs=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-jaxrs/2.13.0-rc2/jackson-datatype-jaxrs-2.13.0-rc2.jar
        com.fasterxml.jackson.datatype.jdk8=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-jdk8/2.13.0-rc2/jackson-datatype-jdk8-2.13.0-rc2.jar
        com.fasterxml.jackson.datatype.joda=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-joda-money/2.13.0-rc2/jackson-datatype-joda-money-2.13.0-rc2.jar
        com.fasterxml.jackson.datatype.jsonorg=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-json-org/2.13.0-rc2/jackson-datatype-json-org-2.13.0-rc2.jar
        com.fasterxml.jackson.datatype.jsonp=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-jakarta-jsonp/2.13.0-rc2/jackson-datatype-jakarta-jsonp-2.13.0-rc2.jar
        com.fasterxml.jackson.datatype.jsr310=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.13.0-rc2/jackson-datatype-jsr310-2.13.0-rc2.jar
        com.fasterxml.jackson.datatype.jsr353=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-jsr353/2.13.0-rc2/jackson-datatype-jsr353-2.13.0-rc2.jar
        com.fasterxml.jackson.jakarta.rs.base=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jakarta/rs/jackson-jakarta-rs-base/2.13.0-rc2/jackson-jakarta-rs-base-2.13.0-rc2.jar
        com.fasterxml.jackson.jakarta.rs.cbor=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jakarta/rs/jackson-jakarta-rs-cbor-provider/2.13.0-rc2/jackson-jakarta-rs-cbor-provider-2.13.0-rc2.jar
        com.fasterxml.jackson.jakarta.rs.json=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jakarta/rs/jackson-jakarta-rs-json-provider/2.13.0-rc2/jackson-jakarta-rs-json-provider-2.13.0-rc2.jar
        com.fasterxml.jackson.jakarta.rs.smile=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jakarta/rs/jackson-jakarta-rs-smile-provider/2.13.0-rc2/jackson-jakarta-rs-smile-provider-2.13.0-rc2.jar
        com.fasterxml.jackson.jakarta.rs.xml=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jakarta/rs/jackson-jakarta-rs-xml-provider/2.13.0-rc2/jackson-jakarta-rs-xml-provider-2.13.0-rc2.jar
        com.fasterxml.jackson.jakarta.rs.yaml=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jakarta/rs/jackson-jakarta-rs-yaml-provider/2.13.0-rc2/jackson-jakarta-rs-yaml-provider-2.13.0-rc2.jar
        com.fasterxml.jackson.jaxrs.base=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jaxrs/jackson-jaxrs-base/2.13.0-rc2/jackson-jaxrs-base-2.13.0-rc2.jar
        com.fasterxml.jackson.jaxrs.cbor=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jaxrs/jackson-jaxrs-cbor-provider/2.13.0-rc2/jackson-jaxrs-cbor-provider-2.13.0-rc2.jar
        com.fasterxml.jackson.jaxrs.json=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jaxrs/jackson-jaxrs-json-provider/2.13.0-rc2/jackson-jaxrs-json-provider-2.13.0-rc2.jar
        com.fasterxml.jackson.jaxrs.smile=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jaxrs/jackson-jaxrs-smile-provider/2.13.0-rc2/jackson-jaxrs-smile-provider-2.13.0-rc2.jar
        com.fasterxml.jackson.jaxrs.xml=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jaxrs/jackson-jaxrs-xml-provider/2.13.0-rc2/jackson-jaxrs-xml-provider-2.13.0-rc2.jar
        com.fasterxml.jackson.jaxrs.yaml=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jaxrs/jackson-jaxrs-yaml-provider/2.13.0-rc2/jackson-jaxrs-yaml-provider-2.13.0-rc2.jar
        com.fasterxml.jackson.jr.annotationsupport=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jr/jackson-jr-annotation-support/2.13.0-rc2/jackson-jr-annotation-support-2.13.0-rc2.jar
        com.fasterxml.jackson.jr.ob=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jr/jackson-jr-objects/2.13.0-rc2/jackson-jr-objects-2.13.0-rc2.jar
        com.fasterxml.jackson.jr.retrofit2=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jr/jackson-jr-retrofit2/2.13.0-rc2/jackson-jr-retrofit2-2.13.0-rc2.jar
        com.fasterxml.jackson.jr.stree=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jr/jackson-jr-stree/2.13.0-rc2/jackson-jr-stree-2.13.0-rc2.jar
        com.fasterxml.jackson.module.afterburner=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/module/jackson-module-afterburner/2.13.0-rc2/jackson-module-afterburner-2.13.0-rc2.jar
        com.fasterxml.jackson.module.blackbird=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/module/jackson-module-blackbird/2.13.0-rc2/jackson-module-blackbird-2.13.0-rc2.jar
        com.fasterxml.jackson.module.guice=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/module/jackson-module-guice/2.13.0-rc2/jackson-module-guice-2.13.0-rc2.jar
        com.fasterxml.jackson.module.jakarta.xmlbind=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/module/jackson-module-jakarta-xmlbind-annotations/2.13.0-rc2/jackson-module-jakarta-xmlbind-annotations-2.13.0-rc2.jar
        com.fasterxml.jackson.module.jaxb=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/module/jackson-module-jaxb-annotations/2.13.0-rc2/jackson-module-jaxb-annotations-2.13.0-rc2.jar
        com.fasterxml.jackson.module.jsonSchema=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/module/jackson-module-jsonSchema/2.13.0-rc2/jackson-module-jsonSchema-2.13.0-rc2.jar
        com.fasterxml.jackson.module.mrbean=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/module/jackson-module-mrbean/2.13.0-rc2/jackson-module-mrbean-2.13.0-rc2.jar
        com.fasterxml.jackson.module.noctordeser=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/module/jackson-module-no-ctor-deser/2.13.0-rc2/jackson-module-no-ctor-deser-2.13.0-rc2.jar
        com.fasterxml.jackson.module.osgi=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/module/jackson-module-osgi/2.13.0-rc2/jackson-module-osgi-2.13.0-rc2.jar
        com.fasterxml.jackson.module.paramnames=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/module/jackson-module-parameter-names/2.13.0-rc2/jackson-module-parameter-names-2.13.0-rc2.jar
        com.fasterxml.jackson.module.paranamer=https://repo.maven.apache.org/maven2/com/fasterxml/jackson/module/jackson-module-paranamer/2.13.0-rc2/jackson-module-paranamer-2.13.0-rc2.jar
        """;
    var properties = new Properties();
    properties.load(new StringReader(modules));
    for (var module : properties.stringPropertyNames()) {
      var found = Jackson.version("2.13.0-rc2").find(module);
      assertTrue(found.isPresent(), module + " not found");
      assertEquals(properties.getProperty(module), found.get(), module);
    }
  }
}
