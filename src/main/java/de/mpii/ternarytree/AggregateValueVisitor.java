package de.mpii.ternarytree;

import java.util.Set;

/**
 * Interface for visiting each node with all the values from the subtree.
 */
public interface AggregateValueVisitor {
  public void visit(int nodeId, Set<Integer> aggregateValues);
}
