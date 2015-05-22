package org.monarch.golr;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.ClassUtils;
import org.monarch.golr.beans.Closure;
import org.monarch.golr.beans.GolrCypherQuery;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

import edu.sdsc.scigraph.frames.CommonProperties;
import edu.sdsc.scigraph.internal.CypherUtil;
import edu.sdsc.scigraph.internal.TinkerGraphUtil;
import edu.sdsc.scigraph.neo4j.Graph;
import edu.sdsc.scigraph.neo4j.GraphUtil;

public class GolrLoader {

  private static String EVIDENCE_GRAPH = "evidence_graph";
  private static String EVIDENCE_FIELD = "evidence_object";

  private final GraphDatabaseService graphDb;
  private final ResultSerializerFactory factory;
  private final EvidenceProcessor processor;
  private final Graph graph;
  private final CypherUtil cypherUtil;

  @Inject
  GolrLoader(GraphDatabaseService graphDb, Graph graph, CypherUtil cypherUtil, ResultSerializerFactory factory, EvidenceProcessor processor) {
    this.graphDb = graphDb;
    this.cypherUtil = cypherUtil;
    this.graph = graph;
    this.factory = factory;
    this.processor = processor;
  }

  long process(GolrCypherQuery query, Writer writer) throws IOException {
    long recordCount = 0;
    try (Transaction tx = graphDb.beginTx()) {
      Result result = cypherUtil.execute(query.getQuery());
      JsonGenerator generator = new JsonFactory().createGenerator(writer);
      ResultSerializer serializer = factory.create(generator);
      generator.writeStartArray();
      while (result.hasNext()) {
        recordCount++;
        generator.writeStartObject();
        Map<String, Object> row = result.next();
        com.tinkerpop.blueprints.Graph evidenceGraph = new TinkerGraph();
        Set<Long> ignoredNodes = new HashSet<>();
        for (Entry<String, Object> entry: row.entrySet()) {
          String key = entry.getKey();
          Object value = entry.getValue();

          if (null == value) {
            continue;
          }
          // Add evidence
          if (value instanceof PropertyContainer) {
            TinkerGraphUtil.addElement(evidenceGraph, (PropertyContainer) value);
          } else if (value instanceof Path) {
            TinkerGraphUtil.addPath(evidenceGraph, (Path) value);
          } 


          if (value instanceof Node) {
            ignoredNodes.add(((Node)value).getId());

            if (query.getCollectedTypes().containsKey(key)) {
              serializer.serialize(key, (Node) value, query.getCollectedTypes().get(key));
            } else {
              serializer.serialize(key, value);
            }
          } else if (value instanceof Relationship) {
            String objectPropertyIri = GraphUtil.getProperty((Relationship) value, CommonProperties.URI, String.class).get();
            Node objectProperty = graphDb.getNodeById(graph.getNode(objectPropertyIri).get());
            serializer.serialize(key, objectProperty);
          } else if (ClassUtils.isPrimitiveOrWrapper(value.getClass()) ||   value instanceof String) {
            // Serialize primitive types and Strings
            serializer.serialize(key, value);
          }
        }
        processor.addAssociations(evidenceGraph);
        serializer.serialize(EVIDENCE_GRAPH, processor.getEvidenceGraph(evidenceGraph));
        Closure closure = processor.getEvidenceIds(evidenceGraph, ignoredNodes);
        serializer.writeArray(EVIDENCE_FIELD + ResultSerializer.ID_SUFFIX, closure.getCuries());
        serializer.writeArray(EVIDENCE_FIELD + ResultSerializer.LABEL_SUFFIX, closure.getLabels());
        closure = processor.entailEvidence(evidenceGraph, ignoredNodes);
        serializer.writeArray(EVIDENCE_FIELD + ResultSerializer.ID_CLOSURE_SUFFIX, closure.getCuries());
        serializer.writeArray(EVIDENCE_FIELD + ResultSerializer.LABEL_CLOSURE_SUFFIX, closure.getLabels());
        generator.writeEndObject();
      }
      generator.writeEndArray();
      generator.close();
      tx.success();
    }
    return recordCount;
  }

}
