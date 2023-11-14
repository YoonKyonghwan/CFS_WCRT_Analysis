package org.cap.model;

public enum ScheduleSimulationMethod {
    BRUTE_FORCE("brute-force"), 
	PRIORITY_QUEUE("priority-queue"),
	RANDOM("random"),
	RANDOM_TARGET_TASK("random_target_task");

    private final String value;

	private ScheduleSimulationMethod(String value) {
		this.value = value;
	}

	public static ScheduleSimulationMethod fromValue(String value) {
		for (ScheduleSimulationMethod c : ScheduleSimulationMethod.values()) {
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
