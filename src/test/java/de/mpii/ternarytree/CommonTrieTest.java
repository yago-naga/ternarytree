package de.mpii.ternarytree;

import static org.junit.Assert.assertEquals;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CommonTrieTest {

    private Class<? extends Trie> clazz;
    private double threshold;
    private Trie t;

    @Parameterized.Parameters(name= "{0}, t = {1}")
    public static Collection<Object[]> classesAndMethods() {
        return Arrays
                .asList(new Object[][] { //{ TernaryTrie.class, 1},
                        { TernaryTriePrimitive.class, 0.8 },
                        { TernaryTriePrimitive.class, 1} });
    }

    public CommonTrieTest(Class<? extends Trie> clazz, double threshold) {
        this.clazz = clazz;
        this.threshold = threshold;
    }

    @Before
    public void initialize() throws NoSuchMethodException, SecurityException,
            IllegalArgumentException, InvocationTargetException {
        try {
            Constructor<? extends Trie> ctor = this.clazz
                    .getConstructor(double.class);
            t = ctor.newInstance(this.threshold);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBasicInsertionAndGetContent() {
        t.put("barack", 0);
        t.put("barack obama", 1);
        t.put("barack obama", 2);
        t.put("barricade", 2);
        
        String content = t.getContent();
        TObjectIntMap<String> hm = new TObjectIntHashMap<String>();
        for (String keyValue : content.split("\n")) {
            hm.put(keyValue.split("\t")[0], Integer.valueOf(keyValue.split("\t")[1]));
        }
        assertEquals(0, hm.get(getPrefixedString("barack")));
        assertEquals(2, hm.get(getPrefixedString("barack obama")));
        assertEquals(2, hm.get(getPrefixedString("barricade")));
        // System.out.println(t.getContent());
    }

    @Test
    public void testGet1() {
        t.put("barack", 0);
        t.put("barack obama", 1);
        t.put("barack obama", 2);
        t.put("barricade", 2);

        assertEquals(2, t.get("barack obama"));
        assertEquals(0, t.get("barack"));
        assertEquals(2, t.get("barricade"));
        // System.out.println(tt.toString());
    }

    @Test
    public void testGet2() {
        t.put("barack", 0);
        t.put("obama", 1);
        t.put("obama", 2);
        t.put("zynga", 2);

        assertEquals(0, t.get("barack"));
        assertEquals(2, t.get("obama"));
        assertEquals(2, t.get("zynga"));
        // System.out.println(tt.toString());
    }

    @Test
    public void testGet3() {
        testGet3Common(false);
    }
    
    @Test
    public void testSerialize3() {
            testGet3Common(true);
    }

    private void testGet3Common(boolean serialize) {
        t.put("the red dog", 0);
        t.put("the red", 1);
        t.put("red king", 2);
        t.put("the new kid", 3);
        t.put("a", 4);
        t.put("my name is earl", 5);
        t.put("saving private ryan", 6);
        t.put("saving", 7);
        t.put("academy award", 8);
        t.put("academy award for best actor", 9);
        
        if(serialize) {
            try{
                File serial = File.createTempFile("serialTrie", "gzip");
                SerializableTrie st = (SerializableTrie)t;
                st.serialize(new FileOutputStream(serial));
                t = st.deserialize(new FileInputStream(serial));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        assertEquals(0, t.get("the red dog"));
        assertEquals(1, t.get("the red"));
        assertEquals(2, t.get("red king"));
        assertEquals(3, t.get("the new kid"));
        assertEquals(4, t.get("a"));
        assertEquals(5, t.get("my name is earl"));
        assertEquals(6, t.get("saving private ryan"));
        assertEquals(7, t.get("saving"));
        assertEquals(8, t.get("academy award"));
        assertEquals(9, t.get("academy award for best actor"));

        assertEquals(-1, t.get("academy awa"));
        assertEquals(-1, t.get("an"));
    }
    
    @Test
    public void testContiguousDelimiters() {
        t.put("the   dog", 0);
        t.put("the    trie", 1);
        assertEquals(0, t.get("the   dog"));
        assertEquals(1, t.get("the    trie"));
        
        assertEquals(-1, t.get("the dog"));
        assertEquals(-1, t.get("the trie"));
    }

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
    public void testRigorous() {
        int length = 18;
        // int length = 15;
        int num = 5;
        Random r = new Random();
        for (int j = 0; j < num; j++) {
            String key = "";
            for (int i = 0; i < length; i++) {
                key += (char) (r.nextInt(26) + 'a');
            }
            int existing = t.get(key);
            assertEquals(-1, existing);
            int value = r.nextInt(100);
            t.put(key, value);
            assertEquals(value, t.get(key));
        }
        // System.out.println(tt.toString());
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
      Match spot = ttp.getLongestMatch(tokens, 0);
      assertEquals(1, spot.getTokenCount());
      
      spot = ttp.getLongestMatch(tokens, 6);
      assertEquals(3, spot.getTokenCount());
      
      spot = ttp.getLongestMatch(tokens, 7);
      assertEquals(0, spot.getTokenCount());
      
      spot = ttp.getLongestMatch(tokens, 14);
      assertEquals(1, spot.getTokenCount());
      
      spot = ttp.getLongestMatch(tokens, 16);
      assertEquals(1, spot.getTokenCount());
      
      spot = ttp.getLongestMatch(tokens, 18);
      assertEquals(1, spot.getTokenCount());
      
      spot = ttp.getLongestMatch(tokens, 24);
      assertEquals(2, spot.getTokenCount());
      
      spot = ttp.getLongestMatch(tokens, 36);
      assertEquals(1, spot.getTokenCount());
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
          + "and was buried at Invalides Invalides .";
      String[] tokens = text.split(" ");
      List<Match> matchedSpots = ttp.getAllMatches(tokens);
      
      assertEquals(8, matchedSpots.size());

      // offset, count, value
      assertEquals(new Match(0, 1, 1) , matchedSpots.get(0));

      assertEquals(new Match(6, 3, 2), matchedSpots.get(1));
      
      assertEquals(new Match(14, 1, 3), matchedSpots.get(2));
      
      assertEquals(new Match(16, 1, 4), matchedSpots.get(3));

      assertEquals(new Match(18, 1, 5), matchedSpots.get(4));
      
      assertEquals(new Match(24, 2, 6), matchedSpots.get(5));
      
      assertEquals(new Match(36, 1, 7), matchedSpots.get(6));
    
      assertEquals(new Match(37, 1, 7), matchedSpots.get(7));
    }
    
    private String getPrefixedString(String key) {
        String[] tokens = key.split(" ");
        String prefixedString = "";
        for (int i = 0; i < tokens.length; i++) {
            int cutLength = (int)Math.ceil(tokens[i].length() * threshold);
            prefixedString += tokens[i].substring(0, cutLength);
            if (i < tokens.length - 1) {
                prefixedString += " ";
            }
        }
        return prefixedString;
    }
}
