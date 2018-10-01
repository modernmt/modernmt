package eu.modernmt.model;

public enum Priority {
    HIGH(0), NORMAL(1), BACKGROUND(2);  //three priority values are allowed

    public final int intValue;

    Priority(int value) {
        this.intValue = value;
    }
}
