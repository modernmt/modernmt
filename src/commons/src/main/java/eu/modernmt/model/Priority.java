package eu.modernmt.model;

public enum Priority {

    HIGH(100),         // translation should be done as soon as possible
    NORMAL(200),       // translation has a normal priority
    BACKGROUND(300);   // translation should not impact normal operation

    public final int intValue;

    Priority(int value) {
        this.intValue = value;
    }

}
