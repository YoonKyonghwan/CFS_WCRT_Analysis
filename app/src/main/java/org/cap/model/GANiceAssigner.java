package org.cap.model;

import java.util.ArrayList;
import java.util.List;

public class GANiceAssigner {
    public List<GAChromosome> chromosomes;
    private int numTask;
    private int targetLatency;
    private int minGranularity;
    private int jiffy_us;
    private int timeout_ms;
    private double mutationRate;

    public GANiceAssigner(int populationSize, int timeout_ms, double mutationRate, int numTask, int targetLatency, int minGranularity, int jiffy_us) {
        //bound the number of chromosomes to 40^numTask
        int num_chromosomes = populationSize;
        if (Math.pow(40, numTask) < populationSize) {
            num_chromosomes = (int) Math.pow(40, numTask);
        }
        this.timeout_ms = timeout_ms;
        this.mutationRate = mutationRate;
        chromosomes = new ArrayList<>(num_chromosomes);
        for (int i = 0; i < num_chromosomes; i++) {
            GAChromosome chromosome = new GAChromosome(numTask, targetLatency, minGranularity, jiffy_us);
            chromosomes.add(chromosome);
        }
        this.numTask = numTask;
        this.targetLatency = targetLatency;
        this.minGranularity = minGranularity;
        this.jiffy_us = jiffy_us;
    }

    public void evolve(List<Core> cores) {
        GAChromosome child1 = new GAChromosome(numTask, targetLatency, minGranularity, jiffy_us);
        GAChromosome child2 = new GAChromosome(numTask, targetLatency, minGranularity, jiffy_us);
        initFitnessInPopulation(cores);
        long startTime = System.currentTimeMillis();
        int additional_iterations = 0;
        while(true) {
            chromosomes.sort((a, b) -> b.fitness - a.fitness);
            if (chromosomes.get(0).fitness == this.numTask) { // break condition
                additional_iterations++;
                if (additional_iterations > (1000 * this.numTask)) {
                    break;
                }
            }
            if (System.currentTimeMillis() - startTime > timeout_ms) {
                break;
            }
            
            GAChromosome parent1 = rwSelection();
            GAChromosome parent2 = rwSelection();
            copyChromosome(parent1, child1);
            copyChromosome(parent2, child2);
            crossover(child1, child2);
            mutate(child1, mutationRate);
            mutate(child2, mutationRate);
            child1.computeFitness(cores);
            child2.computeFitness(cores);
            GAChromosome child = (child1.fitness > child2.fitness) ? child1 : child2;

            // replace
            if (child.fitness > parent1.fitness) {
                copyChromosome(child, parent1);
            } else if (child.fitness > parent2.fitness) {
                copyChromosome(child, parent2);
            } else if (child.fitness > chromosomes.get(chromosomes.size() - 1).fitness) {
                copyChromosome(child, chromosomes.get(chromosomes.size() - 1));
            }
        }

        // set best chromosome to cores
        chromosomes.sort((a, b) -> b.fitness - a.fitness);
        // sort chromosomes by sum of nice values
        GAChromosome bestChromosome = chromosomes.get(0);
        int bestFitness = bestChromosome.fitness;
        for (GAChromosome chromosome : chromosomes) {
            if (chromosome.fitness == bestFitness) {
                if (chromosome.sumOfNiceValues() > bestChromosome.sumOfNiceValues()) {
                    bestChromosome = chromosome;
                }
            }else{
                break;
            }
        }
        bestChromosome.applyNicevalues(cores);
    }

    private void initFitnessInPopulation(List<Core> cores) {
        for (GAChromosome chromosome : chromosomes) {
            chromosome.computeFitness(cores);
        }
    }

    private void copyChromosome(GAChromosome fromChromosome, GAChromosome toChromosome) {
        for (int i = 0; i < numTask; i++) {
            toChromosome.niceValues.set(i, fromChromosome.niceValues.get(i));
        }
        toChromosome.fitness = fromChromosome.fitness;
    }


    private void mutate(GAChromosome child, double mutationRate) {
        for (int i = 0; i < numTask; i++) {
            if (Math.random() < mutationRate) {
                child.niceValues.set(i, (int) (Math.random() * 40) - 20);
            }
        }
        child.shiftNiceTo19();
    }

    // Roulette wheel selection
    private GAChromosome rwSelection() {
        long totalFitness = chromosomes.stream().mapToInt(chromosome -> chromosome.fitness).sum();
        int random = (int) (Math.random() * totalFitness);
        long sum = 0;
        for (GAChromosome chromosome : chromosomes) {
            sum += chromosome.fitness;
            if (sum >= random) {
                return chromosome;
            }
        }
        return chromosomes.get(0); // if not find, return the best chromosome
    }

    private void crossover(GAChromosome child1, GAChromosome child2) {
        // select a random point to crossover (1~numTask-1)
        int crossoverPoint = (int) (Math.random() * (numTask - 1)) + 1;
        for (int i = 0; i < numTask; i++) {
            if (i < crossoverPoint) {
                int tmp = child1.niceValues.get(i);
                child1.niceValues.set(i, child2.niceValues.get(i));
                child2.niceValues.set(i, tmp);
            }
        }
    }
}
