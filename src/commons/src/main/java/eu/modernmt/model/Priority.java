package eu.modernmt.model;

public enum Priority {

    HIGH(100),         // translation should be done as soon as possible
    NORMAL(200),       // translation has a normal priority
    BACKGROUND(300),   // translation should not impact normal operation
    BEST_EFFORT(400);  // translation service does not provide any guarantee that the delivery time meets any quality of service

    public final int intValue;

    Priority(int value) {
        this.intValue = value;
    }

}
