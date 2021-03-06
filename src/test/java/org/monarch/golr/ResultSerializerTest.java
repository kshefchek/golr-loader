package org.monarch.golr;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singleton;
import io.scigraph.neo4j.DirectedRelationshipType;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.neo4j.graphdb.RelationshipType;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class ResultSerializerTest extends GolrLoadSetup {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  StringWriter writer = new StringWriter();
  ResultSerializer serializer;
  JsonGenerator generator;

  @Before
  public void setup() throws Exception {
    generator = new JsonFactory().createGenerator(writer);
    serializer = new ResultSerializer(generator, closureUtil);
    generator.writeStartObject();
  }

  String getActual() throws IOException {
    generator.writeEndObject();
    generator.close();
    return writer.toString();
  }

  @Test
  public void serializePrimitiveTypes() throws Exception {
    serializer.serialize("string", "foo");
    serializer.serialize("boolean", true);
    serializer.serialize("int", 1);
    serializer.serialize("long", 1L);
    serializer.serialize("float", 1.0F);
    serializer.serialize("double", 1.0F);
    JSONAssert.assertEquals(getFixture("fixtures/primitives.json"), getActual(), true);
  }

  // TODO figure out why this test fails
  @Ignore
  @Test
  public void serializeUnknownType() throws Exception {
    exception.expect(IllegalArgumentException.class);
    serializer.serialize("foo", Collections.emptySet());
  }

  @Test
  public void serializeObjectTypes() throws Exception {
    serializer.serialize("string", (Object)"foo");
    serializer.serialize("boolean", (Object)true);
    serializer.serialize("int", (Object)1);
    serializer.serialize("long", (Object)1L);
    serializer.serialize("float", (Object)1.0F);
    serializer.serialize("double", (Object)1.0F);
    JSONAssert.assertEquals(getFixture("fixtures/primitives.json"), getActual(), JSONCompareMode.NON_EXTENSIBLE);
  }

  @Ignore
  @Test
  public void serializeNode() throws Exception {
    serializer.serialize("node", b);
    JSONAssert.assertEquals(getFixture("fixtures/node.json"), getActual(), JSONCompareMode.NON_EXTENSIBLE);
  }

  @Ignore
  @Test
  public void serializeNodeWithDynamicType() throws Exception {
    a.createRelationshipTo(b, RelationshipType.withName("hasPart"));
    serializer.serialize("node", singleton(b), newHashSet(new DirectedRelationshipType("hasPart", "INCOMING")));
    JSONAssert.assertEquals(getFixture("fixtures/node.json"), getActual(), JSONCompareMode.NON_EXTENSIBLE);
  }

}
