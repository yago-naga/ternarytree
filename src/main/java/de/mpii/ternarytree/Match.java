package de.mpii.ternarytree;

public class Match {

    private int tokenCount;
    private int tokenOffset;
    private int value;

    public Match(int tokenOffset, int tokenCount, int value) {
        this.tokenOffset = tokenOffset;
        this.tokenCount = tokenCount;
        this.value = value;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
    
    public int getTokenOffset() {
        return tokenOffset;
    }
    
    @Override
    public boolean equals(Object o) {
        Match m = (Match) o;
        return this.tokenCount == m.getTokenCount()
                && this.tokenOffset == m.getTokenOffset()
                && this.value == m.getValue();
    }
}
