package org.cap.model;

import java.util.ArrayList;
import java.util.List;

import org.cap.simulation.CFSAnalyzer;

public class GAChromosome {
    public List<Integer> niceValues;
    public int fitness;
    private int targetLatency;
    private int minGranularity; 
    private int jiffy_us; // default 1ms

    public GAChromosome(int numTask, int targetLatency, int minGranularity, int jiffy_us) {
        this.niceValues = new ArrayList<>(numTask);
        for (int i = 0; i < numTask; i++) {
            this.niceValues.add((int) (Math.random() * 40) - 20); // initialize with random nice values(-20 ~ 19)
        }
        shiftNiceTo19();
        this.fitness = 0; 
        this.targetLatency = targetLatency;
        this.minGranularity = minGranularity;
        this.jiffy_us = jiffy_us;
    }

    public GAChromosome(int numTask, int targetLatency, int minGranularity, int jiffy_us, List<Integer> niceValues) {
        this.niceValues = new ArrayList<>(numTask);
        for (int i = 0; i < numTask; i++) {
            this.niceValues.add(niceValues.get(i));
        }
        this.fitness = 0; 
        this.targetLatency = targetLatency;
        this.minGranularity = minGranularity;
        this.jiffy_us = jiffy_us;
    }

    public void computeFitness(List<Core> cores){
        applyNicevalues(cores);
        CFSAnalyzer analyzer = new CFSAnalyzer(cores, this.targetLatency, this.minGranularity, this.jiffy_us);
        analyzer.analyze(); 
        int num_schedulable = 0;
        for (Core core : cores) {
            for (Task task : core.tasks) {
                if (task.isSchedulable_by_proposed) {
                    num_schedulable += 1;
                }
            }
        }
        this.fitness = num_schedulable;
    }

    public void shiftNiceTo19(){
        int max_nice = this.niceValues.stream().mapToInt(nice -> nice).max().getAsInt();
        int shift_nice = 19 - max_nice;
        for (int i = 0; i < this.niceValues.size(); i++) {
            this.niceValues.set(i, this.niceValues.get(i) + shift_nice);
        }
    }

    public void applyNicevalues(List<Core> cores){
        for (Core core : cores) {
            for(Task task : core.tasks){
                task.nice = niceValues.get(task.index);
                task.weight = NiceToWeight.getWeight(task.nice);
            }
        }
    }

    public void printNiceValues(){
        for (int nice : this.niceValues) {
            System.out.print(nice + "\t");
        }
        System.out.println();
    }

    public int sumOfNiceValues() {
        return this.niceValues.stream().mapToInt(nice -> nice).sum();
    }
}
