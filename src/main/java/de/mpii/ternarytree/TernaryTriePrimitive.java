package de.mpii.ternarytree;

import gnu.trove.list.TCharList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TCharArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class TernaryTriePrimitive implements Trie, SerializableTrie {
  
    private static final int FORMAT_VERSION = 1;
      
    private TCharList labels = new TCharArrayList();
    private TIntList nodes = new TIntArrayList();
    private int root;
    private double threshold;
    private char delimiter;
    
    public TernaryTriePrimitive() {
      this(1.0);
    }
    
    public TernaryTriePrimitive(double t) {
        this(t, ' ');
    }
    
    public TernaryTriePrimitive(double t, char d) {
        root = -1;
        threshold = t;
        delimiter = d;
    }
        
    public void bulkLoadTrie(Map<String, Integer> items) {
        String[] mentions = items.keySet().toArray(new String[]{});
        Arrays.sort(mentions);
        //Randomly permute
        Random r = new Random();
        for(int i = mentions.length - 1; i > 0; i--) {
            int randomIndex = r.nextInt(i);
            String temp = mentions[i];
            mentions[i] = mentions[randomIndex];
            mentions[randomIndex] = temp;
        }
        for (String mention : mentions) {
            this.put(mention, items.get(mention));
        }
    }
    
    /**
     * Returns all matches found in the input tokens as a map in the form.
     * tokenOffset -> tokenCount
     * 
     * @param tokens
     *            Tokenized text.
     * @return List of Matched SPots
     */
    public List<Match> getAllMatches(String[] tokens) {
        List<Match> matchedSpots = new ArrayList<Match>();
        int i = 0;
        while (i < tokens.length) {
            Match m = getLongestMatch(tokens, i);
            if (m.getTokenCount() > 0) {
                matchedSpots.add(m);
                // Jump after longest match.
                i += m.getTokenCount();
            } else {
                i++;
            }
        }
        return matchedSpots;
    }

    public Match getLongestMatch(String[] tokens, int start) {
        return getLongestMatchAndInternalNodeId(tokens, start, null);
    }

    public Match getLongestMatchAndInternalNodeId(String[] tokens, int start, InternalNodeId nodeId) {
        int node = root;
        int matchValue = -1;
        int matchToken = start - 1;
        int iToken = start;
        for (iToken = start; iToken < tokens.length; iToken++) {
            int pos = 0;
            while (node != -1 && iToken < tokens.length) {
                int relevantLength = getRelevantLength(tokens[iToken]);
                char chr = delimiter;
                if (pos < relevantLength) {
                    chr = tokens[iToken].charAt(pos);
                }
                if (chr < getNodeKey(node)) {
                    node = getLessChild(node);
                } else if (chr == getNodeKey(node)) {
                    if (pos == relevantLength - 1) {
                        if (getNodeValue(node) != -1) {
                            matchValue = getNodeValue(node);
                            matchToken = iToken;
                        }
                    }
                    // Keep track of internal node id if wanted.
                    if (nodeId != null) {
                        nodeId.setId(node);
                    }

                    node = getEqualChild(node);
                    pos++;
                    if (pos > relevantLength) {
                        pos = 0;
                        iToken++;
                    }
                } else {
                    node = getGreatChild(node);
                }
            }
        }

        return new Match(start, matchToken - start + 1, matchValue);
    }
    
    public int get(String[] tokens) {
        Match match = this.getLongestMatch(tokens, 0);
        if (match.getTokenCount() == tokens.length) {
            return match.getValue();
        } else {
            return -1;
        }
    }
        
    public int get(String key) {
        return get(key.split(String.valueOf(delimiter)));
    }

    /**
     * Returns the string that is actually inserted to the tree,
     * respecting prefix thresholding and delimiter.
     *
     * @param s String to insert
     * @return  s in the form it is actually inserted.
     */
    public String getPrefixThresholdedString(String s) {
        String[] split = s.split(String.valueOf(delimiter));
        String[] prefixSplit = new String[split.length];
        for (int i = 0; i < split.length; i++) {
            prefixSplit[i] = split[i].substring(0, getRelevantLength(split[i]));
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < prefixSplit.length; i++) {
            sb.append(prefixSplit[i]);
            if (i < prefixSplit.length - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public int getPrefixId(String partialKey) {
        return getPrefixId(partialKey.split(String.valueOf(delimiter)));
    }

    /**
     * Get the id of the prefix that the given (partial) key points to.
     *
     * @param partialKey    String to look up the prefix id for.
     * @return              Prefix id of the node, or -1 if partialKey is not present.
     */
    public int getPrefixId(String[] partialKey) {
        InternalNodeId nodeId = new InternalNodeId(-1);
        getLongestMatchAndInternalNodeId(partialKey, 0, nodeId);
        return nodeId.getId();
    }

    public void visitAggregateValues(AggregateValueVisitor visitor) {
        visitAggregateValues(visitor, root, new HashSet<Integer>());
    }

    private void visitAggregateValues(AggregateValueVisitor visitor, int node, Set<Integer> parentValues) {
        Set<Integer> values = new HashSet<>();
        if (node != -1) {
            visitAggregateValues(visitor, getLessChild(node), parentValues);
            visitAggregateValues(visitor, getEqualChild(node), values);
            visitAggregateValues(visitor, getGreatChild(node), parentValues);
            int val = getNodeValue(node);
            if (val != -1) {
                values.add(val);
                parentValues.addAll(values);
            }
            visitor.visit(node, values);
        }
    }
    
    public void put(String[] tokens, int value) {
        root = put(root, tokens, 0, 0, value);
    }
    
    public void put(String key, int value) {
        root = put(root, key.split(String.valueOf(delimiter)), 0, 0, value);
    }
    
    private int put(int node, String[] tokens, int iToken, int pos, int value) {
        int length = getRelevantLength(tokens[iToken]);
        char chr = delimiter;
        if (pos < length) {
            chr = tokens[iToken].charAt(pos);
        }
        if (node == -1) {
            node = getNewNode(chr);
        }
        if (chr < getNodeKey(node)) {
            setLessChild(node, put(getLessChild(node), tokens, iToken, pos, value));
        } else if (chr == getNodeKey(node)) {
            if (iToken < tokens.length - 1) {
                if (pos <= length  - 1) {
                    setEqualChild(node, put(getEqualChild(node), tokens, iToken, pos + 1, value));
                } else {
                    setEqualChild(node, put(getEqualChild(node), tokens, iToken + 1, 0, value));
                }
            } else {
                if (pos < length - 1){
                    setEqualChild(node, put(getEqualChild(node), tokens, iToken, pos + 1, value));
                } else {
                    setNodeValue(node, value);
                }
            }

        } else {
             setGreatChild(node, put(getGreatChild(node), tokens, iToken, pos, value));
        }
        return node;
    }
    
    private int getLessChild(int node) {
        return nodes.get(node);
    }
       
    private int getEqualChild(int node) {
        return nodes.get(node + 1);
    }
    
    private int getGreatChild(int node) {
        return nodes.get(node + 2);
    }
    
    private int getNodeValue(int node) {
        return nodes.get(node + 3);
    }
    
    private char getNodeKey(int node) {
        return labels.get(node/4);
    }
    
    private int getNewNode(char chr) {
        int newNode = nodes.size();
        for (int i = 0; i < 4; i++) {
            nodes.add(-1);
        }
        labels.add(chr);
        return newNode;
    }
    
    private void setLessChild(int parentNode, int childNode) {
        nodes.set(parentNode, childNode);
    }
    
    private void setEqualChild(int parentNode, int childNode) {
        nodes.set(parentNode + 1, childNode);
    }
    
    private void setGreatChild(int parentNode, int childNode) {
        nodes.set(parentNode + 2, childNode);
    }
    
    private void setNodeValue(int node, int value) {
        nodes.set(node + 3, value);
    }
    
    public int getTotalNodes() {
        return labels.size();
    }
    
    private void getNodesPerLevel(TIntIntHashMap num, int level, int node) {
        if (node == -1) {
            return;
        } else {
            if ( ! num.containsKey(level)) {
                num.put(level, 1);
            } else {
                num.increment(level);
            }
            getNodesPerLevel(num, level + 1, getLessChild(node));
            getNodesPerLevel(num, level + 1, getEqualChild(node));
            getNodesPerLevel(num, level + 1, getGreatChild(node));
        }
    }
    
    public int[] getNodesPerLevel() {
        TIntIntHashMap num = new TIntIntHashMap();
        getNodesPerLevel(num, 0, root);
        int[] numArray = new int[num.size()];
        for(int key : num.keys()) {
            numArray[key] = num.get(key);
        }
        return numArray;
    }
    
    private void getCollapsableLengths(int node, int length, TIntIntMap distribution) {
        if (node == -1) {
            return;
        }
        if (getEqualChild(node) > 0 && getLessChild(node) < 0 && getGreatChild(node) < 0) {
            getCollapsableLengths(getEqualChild(node), length + 1, distribution);
        } else if (getEqualChild(node) < 0 && getLessChild(node) < 0 && getGreatChild(node) < 0) {
            if (!distribution.containsKey(length)) {
                distribution.put(length, 1);
            } else {
                distribution.increment(length);
            }
        } else {
            getCollapsableLengths(getLessChild(node), 1, distribution);
            getCollapsableLengths(getEqualChild(node), 1, distribution);
            getCollapsableLengths(getGreatChild(node), 1, distribution);
        }
    }
    
    public TIntIntMap getCollapsableLengths() {
        TIntIntMap distribution = new TIntIntHashMap();
        getCollapsableLengths(root, 1, distribution);
        return distribution;
    }
    
    public String getContent() {
        StringBuilder repr = getContent(root, new StringBuilder(), "");
        return repr.toString();
    }
    
    private StringBuilder getContent(int node, StringBuilder repr, String prefix) {
        if (node != -1) {
            if (nodes.get(node + 3) != -1) {
                repr.append(prefix + labels.get(node / 4) + "\t" + String.valueOf(nodes.get(node + 3)) + "\n");
            }
            repr = getContent(nodes.get(node), repr, prefix);
            repr = getContent(nodes.get(node + 1), repr, prefix + labels.get(node / 4));
            repr = getContent(nodes.get(node + 2), repr, prefix);
        }
        return repr;
    }

    public String getTreeView() {
        StringBuilder repr = getTreeView(root, new StringBuilder());
        return repr.toString();
    }

    private StringBuilder getTreeView(int node, StringBuilder sb) {
        if (node != -1) {
            sb.append("[" + node + "] " + labels.get(node/4));
            if (nodes.get(node + 3) != -1) {
                sb.append(": " + String.valueOf(nodes.get(node + 3)));
            }
            sb.append("\n\tl: " + nodes.get(node));
            sb.append("\n\te: " + nodes.get(node + 1));
            sb.append("\n\tr: " + nodes.get(node + 2));
            sb.append("\n\n");
            sb = getTreeView(nodes.get(node), sb);
            sb = getTreeView(nodes.get(node + 1), sb);
            sb = getTreeView(nodes.get(node + 2), sb);
        }
        return sb;
    }

    private int getRelevantLength(String key) {
        return (int)Math.ceil(key.length() * threshold);
    }
    
    public void serialize(OutputStream stream) throws IOException {
        DataOutputStream writer = new DataOutputStream(
                new BufferedOutputStream(stream));
        writer.writeInt(FORMAT_VERSION);
        writer.writeDouble(threshold);
        writer.writeChar(delimiter);
        writer.writeInt(root);
        writer.writeInt(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            writer.writeInt(nodes.get(i));
        }
        writer.writeInt(labels.size());
        for (int i = 0; i < labels.size(); i++) {
            writer.writeChar(labels.get(i));
        }
        writer.flush();
        writer.close();
    }

    public Trie deserialize(InputStream stream) throws IOException {
        DataInputStream reader = new DataInputStream(new BufferedInputStream(stream));        
        nodes.clear();
        labels.clear();
        reader.readInt(); //discard version
        threshold = reader.readDouble();
        delimiter = reader.readChar();
        root = reader.readInt();
        int numNodes = reader.readInt();
        for (int i = 0; i < numNodes; i++) {
            nodes.add(reader.readInt());
        }
        int numLabels = reader.readInt();
        for (int i = 0; i < numLabels; i++) {
            labels.add(reader.readChar());
        }
        return this;
    }

    private class InternalNodeId {
        private int id;

        public InternalNodeId(int id) {
            this.id = id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }
}

