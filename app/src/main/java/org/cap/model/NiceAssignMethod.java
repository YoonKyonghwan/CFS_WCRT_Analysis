package org.cap.model;

public enum NiceAssignMethod {
    FIX_LAMBDA("fix_lambda"), 
	HEURISTIC("heuristic"),
	GENETIC_ALGORITHM("GA");

    private final String value;

	private NiceAssignMethod(String value) {
		this.value = value;
	}

	public static NiceAssignMethod fromValue(String value) {
		for (NiceAssignMethod c : NiceAssignMethod.values()) {
			if (c.value.equals(value)) {
				return c;
			}
		}
		throw new IllegalArgumentException(value.toString());
	}

    @Override
    public String toString() {
		return this.value;
	}
}
