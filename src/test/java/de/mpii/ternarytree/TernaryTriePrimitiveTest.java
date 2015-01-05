package de.mpii.ternarytree;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TernaryTriePrimitiveTest {

  @Test
  public void testBulkLoad() {
    Map<String, Integer> items = new HashMap<String, Integer>();
    items.put("the red dog", 0);
    items.put("the red", 1);
    items.put("red king", 2);
    items.put("the new kid", 3);
    items.put("a", 4);
    items.put("my name is earl", 5);
    items.put("saving private ryan", 6);
    items.put("saving", 7);
    items.put("academy award", 8);
    items.put("academy award for best actor", 9);

    TernaryTriePrimitive ttp = new TernaryTriePrimitive();
    ttp.bulkLoadTrie(items);


    assertEquals(0, ttp.get("the red dog"));
    assertEquals(1, ttp.get("the red"));
    assertEquals(2, ttp.get("red king"));
    assertEquals(3, ttp.get("the new kid"));
    assertEquals(4, ttp.get("a"));
    assertEquals(5, ttp.get("my name is earl"));
    assertEquals(6, ttp.get("saving private ryan"));
    assertEquals(7, ttp.get("saving"));
    assertEquals(8, ttp.get("academy award"));
    assertEquals(9, ttp.get("academy award for best actor"));

    assertEquals(-1, ttp.get("academy awa"));
    assertEquals(-1, ttp.get("an"));
  }

  @Test
  public void testGet5() {
    TernaryTriePrimitive t = new TernaryTriePrimitive();

    t.put("abc", 0);
    t.put("bc", 1);
    t.put("bd", 2);
    t.put("be", 3);
    t.put("bcf", 4);
    t.put("bcd", 5);

    assertEquals(0, t.get("abc"));
    assertEquals(1, t.get("bc"));
    assertEquals(2, t.get("bd"));
    assertEquals(3, t.get("be"));
    assertEquals(4, t.get("bcf"));
  }

  @Test
  public void testGetLongestMatch() {
    TernaryTriePrimitive ttp = new TernaryTriePrimitive();
    ttp.put("Napoleon", 1);
    ttp.put("First French Empire", 2);
    ttp.put("Waterloo", 3);
    ttp.put("Wellington", 4);
    ttp.put("Blücher", 5);
    ttp.put("Saint Helena", 6);
    ttp.put("Invalides", 7);

    String text = "Napoleon was the emperor of the First French Empire . "
            + "He was defeated at Waterloo by Wellington and Blücher . "
            + "He was banned to Saint Helena , died of stomach cancer , "
            + "and was buried at Invalides .";
    String[] tokens = text.split(" ");
    Match Match = ttp.getLongestMatch(tokens, 0);
    assertEquals(1, Match.getTokenCount());

    Match = ttp.getLongestMatch(tokens, 6);
    assertEquals(3, Match.getTokenCount());

    Match = ttp.getLongestMatch(tokens, 7);
    assertEquals(0, Match.getTokenCount());

    Match = ttp.getLongestMatch(tokens, 14);
    assertEquals(1, Match.getTokenCount());

    Match = ttp.getLongestMatch(tokens, 16);
    assertEquals(1, Match.getTokenCount());

    Match = ttp.getLongestMatch(tokens, 18);
    assertEquals(1, Match.getTokenCount());

    Match = ttp.getLongestMatch(tokens, 24);
    assertEquals(2, Match.getTokenCount());

    Match = ttp.getLongestMatch(tokens, 36);
    assertEquals(1, Match.getTokenCount());
  }

  @Test
  public void testGetLongestMatchPartial() {
    TernaryTriePrimitive ttp = new TernaryTriePrimitive();
    ttp.put("A B C", 1);
    ttp.put("A B", 2);

    String text = "A C";
    String[] tokens = text.split(" ");
    Match Match = ttp.getLongestMatch(tokens, 0);
    assertEquals(0, Match.getTokenCount());
    assertEquals(-1, Match.getValue());
  }

  @Test
  public void testGetAllMatches() {
    TernaryTriePrimitive ttp = new TernaryTriePrimitive();
    ttp.put("Napoleon", 1);
    ttp.put("First French Empire", 2);
    ttp.put("Waterloo", 3);
    ttp.put("Wellington", 4);
    ttp.put("Blücher", 5);
    ttp.put("Saint Helena", 6);
    ttp.put("Invalides", 7);

    String text = "Napoleon was the emperor of the First French Empire . "
            + "He was defeated at Waterloo by Wellington and Blücher . "
            + "He was banned to Saint Helena , died of stomach cancer , "
            + "and was buried at Invalides .";
    String[] tokens = text.split(" ");
    List<Match> matchedMatchs = ttp.getAllMatches(tokens);

    assertEquals(7, matchedMatchs.size());

    // offset, count, value
    assertEquals(new Match(0, 1, 1), matchedMatchs.get(0));

    assertEquals(new Match(6, 3, 2), matchedMatchs.get(1));

    assertEquals(new Match(14, 1, 3), matchedMatchs.get(2));

    assertEquals(new Match(16, 1, 4), matchedMatchs.get(3));

    assertEquals(new Match(18, 1, 5), matchedMatchs.get(4));

    assertEquals(new Match(24, 2, 6), matchedMatchs.get(5));

    assertEquals(new Match(36, 1, 7), matchedMatchs.get(6));
  }

  @Test
  public void testAggregatedValuesVisitor() {
    TernaryTriePrimitive ttp = new TernaryTriePrimitive();
    ttp.put("a", 0);
    ttp.put("ab", 1);
    ttp.put("abc", 2);
    ttp.put("ac", 3);
    ttp.put("b", 4);

    AggregateValueVisitor avv = new AggregateValueVisitor() {
      @Override
      public void visit(int nodeId, Set<Integer> aggregateValues) {
        if (aggregateValues.contains(0)) {
          assertTrue(aggregateValues.contains(1));
          assertTrue(aggregateValues.contains(2));
          assertTrue(aggregateValues.contains(3));
        }
        if (aggregateValues.contains(1) &&
                !aggregateValues.contains(0)) {
          assertTrue(aggregateValues.contains(2));
          assertFalse(aggregateValues.contains(3));
          assertFalse(aggregateValues.contains(4));
        }
        if (aggregateValues.contains(2)) {
          Set<Integer> a = new HashSet<>();
          a.add(2);
          Set<Integer> b = new HashSet<>();
          b.add(2);
          b.add(1);
          Set<Integer> c = new HashSet<>();
          c.add(2);
          c.add(1);
          c.add(0);
          c.add(3);
          Set<Integer> d = new HashSet<>();
          d.add(2);
          d.add(1);
          d.add(0);
          d.add(3);
          d.add(4);
          assertTrue(
                  aggregateValues.equals(a) ||
                          aggregateValues.equals(b) ||
                          aggregateValues.equals(c) ||
                          aggregateValues.equals(d)
          );
        }
      }
    };

    ttp.visitAggregateValues(avv);
  }
}
