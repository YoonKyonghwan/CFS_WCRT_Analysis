package org.cap.simulation.comparator;

public enum ComparatorCase {
    BODY_WCET("BodyWCETComparator"), 
	PERIOD("PeriodComparator"), 
	WEIGHT("WeightComparator"),
    TARGET_TASK("TargetTaskBasedComparator"),
    RELEASE_TIME("ReleaseTimeComparator"),
    FIFO("FIFOComparator"),
    UNORDERED("UnorderedComparator"),
    INITIAL_ORDER("InitialOrderBasedComparator");
	
	private final String value;
	
	ComparatorCase(String compareCase) {
        this.value = compareCase;
    }

    public static ComparatorCase fromValue(String value) {
		for (ComparatorCase c : ComparatorCase.values()) {
			if (c.value.equals(value)) {
				return c;
			}
		}
		throw new IllegalArgumentException(value.toString());
	}
	
	public String getClassName() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
