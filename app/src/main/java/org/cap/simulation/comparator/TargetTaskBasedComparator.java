package org.cap.simulation.comparator;

import org.cap.model.Task;

public class TargetTaskBasedComparator implements BasicTaskComparator {

    boolean reverseOrder;

    public TargetTaskBasedComparator() {
        this.reverseOrder = false;
    }

    public TargetTaskBasedComparator(boolean reverseOrder) {
        this.reverseOrder = reverseOrder;
    }

    public int compareNotTargetTask(Task o1, Task o2) {
        return 0;
    }

    @Override
    public int compareTie(Task o1, Task o2) {
    if(o1.isTargetTask == true && o2.isTargetTask == false) {
            return 1;
        }
        else if(o1.isTargetTask == false && o2.isTargetTask == true) {
            return -1;
        }
        else {
            if(this.reverseOrder == true) {
                return -compareNotTargetTask(o1, o2);
            } else {
                return compareNotTargetTask(o1, o2);
            }
            
        }
    }
}
