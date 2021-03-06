package org.monarch.golr;

import io.scigraph.frames.CommonProperties;
import io.scigraph.frames.NodeProperties;
import io.scigraph.owlapi.OwlRelationships;
import io.scigraph.owlapi.curies.CurieUtil;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class GolrLoadSetup extends io.scigraph.util.GraphTestBase {

  static Node a, b, c, d, e, f;
  static CurieUtil curieUtil;
  static ClosureUtil closureUtil;
  static Map<String, String> curieMap = new HashMap<>();

  static String getFixture(String name) throws IOException {
    URL url = Resources.getResource(name);
    return Resources.toString(url, Charsets.UTF_8);
  }

  static void populateGraph(GraphDatabaseService graphDb) {
    try (Transaction tx = graphDb.beginTx()) {
      Relationship r1 = addRelationship("http://x.org/a_a", "http://x.org/a_b", OwlRelationships.RDFS_SUBCLASS_OF);
      Relationship r2 = addRelationship("http://x.org/a_b", "http://x.org/a_c", OwlRelationships.RDFS_SUBCLASS_OF);
      Relationship r3 = addRelationship("http://x.org/a_c", "http://x.org/a_d", OwlRelationships.RDF_TYPE);
      Relationship r4 = addRelationship("http://x.org/a_e", "http://x.org/a_d", RelationshipType.withName("CAUSES"));
      Relationship r5 = addRelationship("http://x.org/a_f", "http://x.org/a_e", RelationshipType.withName("partOf"));
      graph.setRelationshipProperty(r4.getId(), CommonProperties.IRI, "http://x.org/a_causes");
      addRelationship("http://x.org/a_causes_parent", "http://x.org/a_causes", OwlRelationships.RDFS_SUB_PROPERTY_OF);
      addRelationship("_:anon", "http://x.org/a_b", OwlRelationships.RDFS_SUBCLASS_OF);
      r1.getEndNode().setProperty(NodeProperties.LABEL, "A");
      r2.getStartNode().setProperty(NodeProperties.LABEL, "C");
      a = r1.getEndNode();
      b = r2.getEndNode();
      c = r2.getStartNode();
      d = r3.getStartNode();
      e = r4.getEndNode();
      f = r5.getEndNode();
      Node assn = createNode("http://x.org/a_assn");
      Node assnParent = createNode("http://x.org/a_assn_parent");
      assn.createRelationshipTo(assnParent, OwlRelationships.RDFS_SUBCLASS_OF);
      Node evidence = createNode("http://x.org/a_evidence");
      assn.createRelationshipTo(evidence, RelationshipType.withName("http://purl.obolibrary.org/obo/RO_0002558"));
      assn.createRelationshipTo(d, RelationshipType.withName("http://purl.org/oban/association_has_subject"));
      assn.createRelationshipTo(e, RelationshipType.withName("http://purl.org/oban/association_has_object"));
      
      Node gene = createNode("http://x.org/gene");
      gene.addLabel(Label.label("gene"));
      Node ortholog = createNode("http://x.org/gene_ortholog");
      ortholog.addLabel(Label.label("gene"));
      gene.createRelationshipTo(ortholog, RelationshipType.withName("http://purl.obolibrary.org/obo/RO_HOM0000017"));
      tx.success();
    }
  }

  @BeforeClass
  public static void buildGraph() {
    populateGraph(graphDb);
    curieMap.put("X", "http://x.org/a_");
    curieUtil = new CurieUtil(curieMap);
    closureUtil = new ClosureUtil(graphDb, curieUtil);
  }

}
