package org.monarch.golr;

import static java.util.Collections.emptyList;
import io.scigraph.bbop.BbopGraphUtil;
import io.scigraph.internal.GraphAspect;
import io.scigraph.owlapi.curies.CurieUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.monarch.golr.beans.Closure;
import org.neo4j.graphdb.GraphDatabaseService;

import com.tinkerpop.blueprints.Graph;

public class EvidenceProcessorStub extends EvidenceProcessor {

  EvidenceProcessorStub(GraphDatabaseService graphDb, GraphAspect aspect, ClosureUtil closureUtil) {
    super(graphDb, aspect, closureUtil, new BbopGraphUtil(new CurieUtil(
        new HashMap<String, String>())));
  }

  @Override
  void addAssociations(Graph graph) {}

  @Override
  String getEvidenceGraph(Graph graph) {
    return "foo";
  }

  @Override
  List<Closure> getEvidenceObject(Graph graph, Set<Long> ignoredNodes) {
    return emptyList();
  }

}
