package org.cap.simulation;

import org.cap.model.*;
import org.cap.simulation.comparator.BasicTaskComparator;
import org.cap.simulation.comparator.ComparatorCase;
import org.cap.utility.LoggerUtility;
import org.cap.utility.MathUtility;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.Map.Entry;

public class CFSSimulator {
    private static final Logger logger = LoggerUtility.getLogger();
    ScheduleSimulationMethod method;
    ComparatorCase comparatorCase;
    private long targetLatency;
    private long minimumGranularity = 1 * 1000L;
    private long triedScheduleCount = 0;
    private ScheduleCache scheduleCache;
    private long numOfTryToSchedule;

    public CFSSimulator(ScheduleSimulationMethod method, ComparatorCase comparatorCase, int targetLatency, long numOfTryToSchedule) {
        this.method = method;
        this.comparatorCase = comparatorCase;
        this.targetLatency = targetLatency * 1000L;
        this.triedScheduleCount = 0;
        this.scheduleCache = new ScheduleCache();
        this.numOfTryToSchedule = numOfTryToSchedule;
    }
    
    public long getTriedScheduleCount() {
        return triedScheduleCount;
    }

    private long getMaximumPeriod(List<Core> cores) {
        long maxPeriod = -1;
        for (Core core : cores) {
            for (Task task : core.tasks) {
                if (task.period > maxPeriod)
                    maxPeriod = task.period;
            }
        }

        return maxPeriod;
    }

    public SimulationResult mergeToFinalResult(SimulationResult finalResult, SimulationResult resultToAdd) {
        if(resultToAdd.isSchedulability() == false) {
                finalResult.setSchedulability(false);
        }

        for(Entry<Integer, Long> entry: resultToAdd.getWcrtMap().entrySet()) {
            if(!finalResult.getWcrtMap().containsKey(entry.getKey())) {
                finalResult.getWcrtMap().put(entry.getKey(), entry.getValue());
            } else {
                if (finalResult.getWcrtMap().get(entry.getKey()).longValue() < entry.getValue().longValue()) {
                    finalResult.getWcrtMap().put(entry.getKey(), entry.getValue());
                }
            }
        }

        return finalResult;
    }

    public SimulationResult simulateCFS(List<Core> cores, int targetTaskID, long simulationTime)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.fine("\n------------------------------");
        logger.fine("*** CFS Simulation Started ***");
        logger.fine("------------------------------");
        SimulationResult finalSimulationResult;
        HashMap<Integer, Long> wcrtMap = initializeWCRTs(cores);
        List<Queue<TaskStat>> queues = initializeQueues(cores, targetTaskID);
        CFSSimulationState simulationState = new CFSSimulationState(this.targetLatency, this.minimumGranularity,
                cores.size(), this.method);
        long time = 0;
        this.triedScheduleCount = 0;
        this.scheduleCache = new ScheduleCache();

        if(simulationTime == 0) { // hyper period
            simulationTime = MathUtility.getLCM(cores);
        }
        else if (simulationTime == -1) {
            simulationTime = getMaximumPeriod(cores);
        }

        simulationState.insertPeriodsAtStartTime(cores);
        //simulationState.insertPeriodsIntoEventQueue(simulationTime, cores);
        simulationState.setPreviousEventTime(time);
        long previousTime = simulationState.getPreviousEventTime();
        time = simulationState.peekNextEventTime();
        simulationState.insertPeriodsInTimeRange(cores, previousTime, time);
        time = simulationState.getNextEventTime();

        if(this.method != ScheduleSimulationMethod.RANDOM) {
            this.numOfTryToSchedule = 1;
            finalSimulationResult = performSimulation(cores, queues, wcrtMap, simulationState, time, simulationTime, 0);
        } else {
            finalSimulationResult = performSimulation(cores, queues, wcrtMap, simulationState, time, simulationTime, 0);
            for(int i = 0 ; i < this.numOfTryToSchedule - 1 ; i++) {
                SchedulePickResult pickResult = this.scheduleCache.pickScheduleData();
                ScheduleCacheData pickData = pickResult.getScheduleData();
                SimulationResult simulResult = performSimulation(cores, pickData.getQueues(), wcrtMap,
                        pickData.getSimulationState(), pickData.getTime(), simulationTime, pickData.getCoreIndex());
                finalSimulationResult = mergeToFinalResult(finalSimulationResult, simulResult);                
            }
        }
        logger.fine("\n------------------------------");
        logger.fine("******** Final Result ********");
        logger.fine("------------------------------");
        return finalSimulationResult;
    }

    private SimulationResult performSimulation(List<Core> cores, List<Queue<TaskStat>> queues, HashMap<Integer, Long> wcrtMap,
            CFSSimulationState simulationState, long time, long simulationTime, int coreIndex)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        boolean diverged = false;
        ScheduleCacheData scheduleData = null;
        List<TaskStat> minRuntimeTasks = null;
        SimulationResult mergedResult = new SimulationResult(true, wcrtMap);

        HashMap<Integer, Long> cloneWcrtMap = cloneHashMap(wcrtMap);

        do {
            outerLoop:
            while (time < simulationTime && time >= 0) {
                logger.finer("\nTime " + time + ":");
                addJobs(cores, queues, simulationState.coreStates, time);

                List<TaskStat> blockingTasks = getBlockingTasks(queues, simulationState);
                if (blockingTasks.size() > 1) {
                    pathDivergesBlocking(blockingTasks, cores, queues, cloneWcrtMap, simulationState, time, simulationTime);
                    break outerLoop;
                }

                simulationState.blockingPolicyReset = false;
                for (; coreIndex < cores.size(); coreIndex++) {
                    Queue<TaskStat> queue = queues.get(coreIndex);
                    CoreState coreState = simulationState.coreStates.get(coreIndex);

                    TaskStat task = null;
                    if (coreState.isRunning) {
                        task = coreState.currentTask;
                        executeTask(task, queue, cloneWcrtMap, simulationState, coreState, time, coreIndex);
                        updateMinimumVirtualRuntime(coreState, queue);
                        if(queue.size() == 0) {
                            coreState.minimumVirtualRuntime = task.virtualRuntime;
                        }
                    }
                    if(coreState.isRunning == false) {
                        task = null;
                        minRuntimeTasks = getMinRuntimeTasks(queue, simulationState, time);
                        if (simulationState.getMethod() == ScheduleSimulationMethod.PRIORITY_QUEUE) {
                            if (minRuntimeTasks.size() > 0)
                                task = minRuntimeTasks.get(0);
                        } else { // BRUTE_FORCE or RANDOM
                            if (minRuntimeTasks.size() > 1) {
                                long scheduleID = this.scheduleCache.pushScheduleData(simulationState.getSimulationScheduleID(), queues, cloneWcrtMap, simulationState, time, minRuntimeTasks, coreIndex, comparatorCase);
                                int taskIndexToSelect = pickNextTaskToSchedule(simulationState, scheduleID);
                                if(taskIndexToSelect == -1) {
                                    diverged = true;
                                    break outerLoop;
                                } else {
                                    task = minRuntimeTasks.get(taskIndexToSelect);
                                    minRuntimeTasks.remove(taskIndexToSelect);
                                    queue.addAll(minRuntimeTasks);
                                }
                            }
                            else if (minRuntimeTasks.size() == 1)
                                task = minRuntimeTasks.get(0);
                        }
                        if (task == null)
                            continue;
                        else
                            logger.fine("Task " + task.task.id + "(vruntime:" + task.virtualRuntime + ") started to run at time " + time);
                        setRuntime(coreIndex, task, queue, simulationState);
                        simulationState.putEventTime((time + coreState.remainingRuntime));
                        coreState.currentTask = task;
                    }
                }
                coreIndex = 0;
                if (simulationState.blockingPolicyReset)
                    simulationState.blockingPolicy = BlockingPolicy.NONE;

                simulationState.setPreviousEventTime(time);
                long previousEventTime = simulationState.getPreviousEventTime();
                time = simulationState.peekNextEventTime();
                if( time == -1) {
                    simulationState.insertNextPeriodsBeforeSimulationTime(cores, previousEventTime, simulationTime);
                } else {
                    simulationState.insertPeriodsInTimeRange(cores, previousEventTime, time);
                }
                time = simulationState.getNextEventTime();
                //logger.info("time popped up: " + time);
            }

            if (diverged == true) {
                for (coreIndex = 0; coreIndex < cores.size(); coreIndex++) {
                    Queue<TaskStat> queue = queues.get(coreIndex);
                    queue.clear();
                }
            } else {
                this.triedScheduleCount++;
                if(this.triedScheduleCount % 10000 == 0) {
                    logger.fine("tried: " + this.triedScheduleCount);
                }

                SimulationResult simulResult = checkSchedulability(cores, queues, cloneWcrtMap);
                mergedResult = mergeToFinalResult(mergedResult, simulResult);

                if(simulationState.getMethod() != ScheduleSimulationMethod.PRIORITY_QUEUE) { 
                    this.scheduleCache.saveFinalScheduledIndex(simulationState.getSimulationScheduleID(), simulationState);
                    if(simulationState.getMethod() == ScheduleSimulationMethod.RANDOM) {
                        this.scheduleCache.clearStack();
                        break;
                    }
                }
            }
            if(this.scheduleCache.getScheduleStackSize() > 0) {
                scheduleData = this.scheduleCache.popScheduleData().copy();
                queues = scheduleData.getQueues();
                simulationState = scheduleData.getSimulationState();
                time = scheduleData.getTime();
                minRuntimeTasks = scheduleData.getMinRuntimeTasks();
                coreIndex = scheduleData.getCoreIndex();
            }
        } while(this.scheduleCache.getScheduleStackSize() > 0);

        return mergedResult;
    }

    private void addIntoQueue(Queue<TaskStat> queue, TaskStat task, long time) {
        task.setQueueInsertTime(time);
        queue.add(task);
    }

    private void addAllIntoQueue(Queue<TaskStat> queue, List<TaskStat> tasks, long time) {
        for(TaskStat task : tasks) {
            task.setQueueInsertTime(time);
        }
        queue.addAll(tasks);
    }

    private void executeTask(TaskStat task, Queue<TaskStat> queueInCore, HashMap<Integer, Long> wcrtMap,
            CFSSimulationState simulationState, CoreState coreState, long time, int coreIndex) {
        logger.finer("-Core: " + coreIndex + ", Task " + task.task.id + " executed in stage: " + task.stage);
        long timeUpdated = time - simulationState.getPreviousEventTime();
        // Decrease runtime
        coreState.remainingRuntime -= timeUpdated;

        // Update virtual runtime
        // long temp = (long) (Math.ceil(((timeUpdated << 10L)  / task.weight) /100) *100);
        // task.virtualRuntime += temp ;

        long vruntime_increment = (timeUpdated << 10L)  / task.task.weight;
        // long vruntime_increment = (timeUpdated << 10L) * NiceToWeight.getWeightMul(task.nice) >> 32L;
        task.virtualRuntime += vruntime_increment;
        logger.fine("Task " + task.task.id + " spends " + timeUpdated + " ns from " + simulationState.getPreviousEventTime() + " to " + time + "[vruntime_increment: " + vruntime_increment + "]");


        // Decrease execution time for each stage
        while(timeUpdated > 0 && task.stage != Stage.COMPLETED) {
            switch (task.stage) {
                case READ:
                    if(task.readTimeInNanoSeconds > timeUpdated) {
                        task.readTimeInNanoSeconds -= timeUpdated;
                        timeUpdated = 0;
                    } else {
                        task.readTimeInNanoSeconds = 0;
                        timeUpdated -= task.readTimeInNanoSeconds;
                    }
                    if (task.readTimeInNanoSeconds <= 0) {
                        task.stage = Stage.BODY;
                        task.bodyReleaseTime = time + 1;
                        simulationState.blockingPolicyReset = true;
                    }
                    else {
                        if (coreState.isRunning)
                            simulationState.blockingPolicy = BlockingPolicy.READ;
                    }
                    break;
                case BODY:
                    if(task.bodyTimeInNanoSeconds > timeUpdated) {
                        task.bodyTimeInNanoSeconds -= timeUpdated;
                        timeUpdated = 0;
                    } else {
                        task.bodyTimeInNanoSeconds = 0;
                        timeUpdated -= task.bodyTimeInNanoSeconds;
                    }
                    if (task.bodyTimeInNanoSeconds <= 0) {
                        if (task.writeTimeInNanoSeconds > 0) {
                            task.stage = Stage.WRITE;
                            task.writeReleaseTime = time + 1;
                        }
                        else {
                            task.stage = Stage.COMPLETED;
                        }
                    }
                    break;
                case WRITE:
                    if(task.writeTimeInNanoSeconds > timeUpdated) {
                        task.writeTimeInNanoSeconds -= timeUpdated;
                        timeUpdated = 0;
                    } else {
                        task.writeTimeInNanoSeconds = 0;
                        timeUpdated -= task.writeTimeInNanoSeconds;
                    }
                    if (task.writeTimeInNanoSeconds <= 0) {
                        task.stage = Stage.COMPLETED;
                        simulationState.blockingPolicyReset = true;
                    }
                    else
                        simulationState.blockingPolicy = BlockingPolicy.WRITE;
                    break;
                case COMPLETED:
                    logger.severe("Task " + task.task.id + " entered with completed stage.");
                    break;
                default:
                    break;
            }
        }

        // Add task back to queue if task is not finished but runtime is over
        if (task.stage != Stage.COMPLETED) {
            // handling the preemption from other tasks becuase of new task invocation
            // if time is still remained (which means the task cannot be executed until the time slice), clear the next end event of this task
            if(coreState.remainingRuntime > 0) { 
                simulationState.clearEventTime(time + coreState.remainingRuntime);
            }
            addIntoQueue(queueInCore, task, time);
            if (simulationState.blockingPolicy == BlockingPolicy.READ)
                simulationState.blockingPolicyReset = true;
        } else {
            logger.fine("Task " + task.task.id + " completed at time " + time + " with RT "
                    + (time - task.readReleaseTime));
            wcrtMap.put(Integer.valueOf(task.getId()),
                    Math.max(wcrtMap.get(Integer.valueOf(task.getId())), time - task.readReleaseTime));
            coreState.putFinishedTaskVirtualRuntime(task.task.id, task.virtualRuntime);
        }
        coreState.isRunning = false;
    }

    private void updateMinimumVirtualRuntime(CoreState coreState, Queue<TaskStat> queue) {
        if (queue.size() >= 1)
            coreState.minimumVirtualRuntime = queue.peek().virtualRuntime;
    }

    private HashMap<Integer, Long> initializeWCRTs(List<Core> cores) {
        HashMap<Integer, Long> wcrtMap = new HashMap<Integer, Long>();

        for (Core core : cores) {
            for (Task task : core.tasks) {
                wcrtMap.put(Integer.valueOf(task.getId()), 0L);
            }
        }
        return wcrtMap;
    }

    private List<Queue<TaskStat>> initializeQueues(List<Core> cores, int targetTaskID)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<Queue<TaskStat>> queues = new ArrayList<>();
        for (Core core : cores) {
            Class<?> clazz = Class
                    .forName(this.getClass().getPackage().getName() + ".comparator." + comparatorCase.getClassName());
            Constructor<?> ctor = clazz.getConstructor();
            BasicTaskComparator taskComparator = (BasicTaskComparator) ctor.newInstance(new Object[] {});

            Queue<TaskStat> queueInCore = new PriorityQueue<>(taskComparator);
            for (Task task : core.tasks) {
                if (targetTaskID == task.id)
                    task.isTargetTask = true;
                else
                    task.isTargetTask = false;

                TaskStat taskStat = new TaskStat(task);
                taskStat.readReleaseTime = task.startTime;
                taskStat.virtualRuntime = 0L;
                
                skipReadStageIfNoReadTime(taskStat);
                if (task.startTime == 0L) {
                    addIntoQueue(queueInCore, taskStat.copy(), 0L);
                }
            }
            queues.add(queueInCore);
        }
        return queues;
    }

    private void addJobs(List<Core> cores, List<Queue<TaskStat>> queues, List<CoreState> coreStates, long time) {
        for (Core core : cores) {
            CoreState coreState = coreStates.get(core.coreID - 1);
            Queue<TaskStat> queue = queues.get(core.coreID - 1);

            for (Task task : core.tasks) {
                if (initialJobs(time, task) || periodicJobs(time, task)) {
                    logger.fine("Tasks " + task.id + " Released at time " + time);
                    // logger.fine("- Task " + task.id + " (Read Time: " + task.readTimeInNanoSeconds + ", Body Time: " + task.bodyTimeInNanoSeconds
                            // + ", Write Time: " + task.writeTimeInNanoSeconds + ")");
                    TaskStat taskStat = new TaskStat(task);

                    taskStat.readReleaseTime = time;
                    taskStat.virtualRuntime = Math.max(coreState.getLastVirtualRuntime(task.id), coreState.minimumVirtualRuntime);
                    skipReadStageIfNoReadTime(taskStat);
                    addIntoQueue(queue, taskStat, time);
                }
            }
        }
    }

    private boolean initialJobs(long time, Task task) {
        return task.startTime > 0 && time == task.startTime;
    }

    private boolean periodicJobs(long time, Task task) {
        return time > task.startTime && task.period > 0 && (time - task.startTime) % task.period == 0;
    }

    private void skipReadStageIfNoReadTime(TaskStat task) {
        if (task.stage == Stage.READ && task.readTimeInNanoSeconds <= 0) {
            task.stage = Stage.BODY;
            task.bodyReleaseTime = task.readReleaseTime;
        }
    }

    private void pathDivergesBlocking(List<TaskStat> blockingTasks, List<Core> cores, List<Queue<TaskStat>> queues,
            HashMap<Integer, Long> wcrtMap, CFSSimulationState simulationState, long time, long hyperperiod)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<HashMap<Integer, Long>> possibleWCRTs = new ArrayList<>();
        for (int i = 0; i < blockingTasks.size(); i++) {
            if (blockingTasks.get(i).stage == Stage.READ)
                simulationState.blockingPolicy = BlockingPolicy.READ;
            else
                simulationState.blockingPolicy = BlockingPolicy.WRITE;

            simulationState.blockingTaskId = blockingTasks.get(i).task.id;
            possibleWCRTs.add(simulatePathBlocking(cores, queues, wcrtMap, simulationState, time, hyperperiod));
        }

        for (Entry<Integer, Long> entry : wcrtMap.entrySet()) {
            long maxWCRT = 0;
            for (HashMap<Integer, Long> possibleWcrtMap : possibleWCRTs) {
                maxWCRT = Math.max(maxWCRT, possibleWcrtMap.get(entry.getKey()).longValue());
            }
            wcrtMap.put(entry.getKey(), Long.valueOf(maxWCRT));
        }
    }

    private int pickNextTaskToSchedule(CFSSimulationState simulationState, long scheduleId) {
        SchedulePickResult pickResult;
        if(simulationState.getMethod() == ScheduleSimulationMethod.BRUTE_FORCE) {
            pickResult = this.scheduleCache.pickScheduleDataByEntry(scheduleId, false);
        } else { // RANDOM
            pickResult = this.scheduleCache.pickScheduleDataByEntry(scheduleId, true);
        }

        if(pickResult != null) {
            simulationState.setSelectedDivergeIndex(pickResult.getDivergeIndex());
            simulationState.setSimulationScheduleID(scheduleId);
            return pickResult.getDivergeIndex();
        } else {
            return -1;
        }        
    }

    private SimulationResult pathDivergesEqualMinRuntime(int coreIndex, List<TaskStat> minRuntimeTasks, List<Core> cores,
            List<Queue<TaskStat>> queues, HashMap<Integer, Long> wcrtMap, CFSSimulationState simulationState, long time,
            long hyperperiod) throws ClassNotFoundException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<SimulationResult> possibleWCRTs = new ArrayList<>();
        SimulationResult mergedResult = new SimulationResult(true, wcrtMap);
        if(simulationState.getMethod() == ScheduleSimulationMethod.BRUTE_FORCE) {
            for (int i = 0; i < minRuntimeTasks.size(); i++)
                possibleWCRTs.add(simulatePathEqualMinRuntime(cores, queues, wcrtMap, simulationState, time, hyperperiod,
                        minRuntimeTasks, -1L, i, coreIndex));
        } else {  // RANDOM
            long scheduleId = this.scheduleCache.saveIntermediateScheduleData(simulationState.getSimulationScheduleID(), cores, queues, wcrtMap, simulationState, time, minRuntimeTasks, coreIndex);
            SchedulePickResult pickResult = this.scheduleCache.pickScheduleDataByEntry(scheduleId, true);

            queues = pickResult.getScheduleData().getQueues();
            simulationState = pickResult.getScheduleData().getSimulationState();
            time = pickResult.getScheduleData().getTime();
            minRuntimeTasks = pickResult.getScheduleData().getMinRuntimeTasks();
            coreIndex = pickResult.getScheduleData().getCoreIndex();
        
            possibleWCRTs.add(simulatePathEqualMinRuntime(cores, queues, wcrtMap, simulationState, time, hyperperiod,
                        minRuntimeTasks, scheduleId , pickResult.getDivergeIndex(), coreIndex));
        }

        for (Entry<Integer, Long> entry : mergedResult.getWcrtMap().entrySet()) {
            long maxWCRT = 0;
            for (SimulationResult simulationResult : possibleWCRTs) {
                maxWCRT = Math.max(maxWCRT, simulationResult.getWcrtMap().get(entry.getKey()).longValue());
                if (simulationResult.isSchedulability() == false) {
                    mergedResult.setSchedulability(false);
                }
            }

            if(entry.getValue().longValue() < maxWCRT) {
                mergedResult.getWcrtMap().put(entry.getKey(), maxWCRT);
            }
        }

        return mergedResult;
    }

    private void setRuntime(int coreIndex, TaskStat task, Queue<TaskStat> queueInCore, CFSSimulationState simulationState) {
        CoreState coreState = simulationState.coreStates.get(coreIndex);
        long totalWeight = queueInCore.stream().mapToLong(t -> t.task.weight).sum() + task.task.weight;
        coreState.remainingRuntime = (int) Math.max(simulationState.targetedLatency * task.task.weight / totalWeight,
                simulationState.minimumGranularity);
        coreState.remainingRuntime = Math.min(coreState.remainingRuntime, (int) (task.readTimeInNanoSeconds + task.bodyTimeInNanoSeconds + task.writeTimeInNanoSeconds));
        coreState.isRunning = true;
    }

    private List<TaskStat> getBlockingTasks(List<Queue<TaskStat>> queues, CFSSimulationState simulationState) {
        if (simulationState.blockingPolicy != BlockingPolicy.NONE)
            return new ArrayList<>();

        List<TaskStat> readWriteTasks = new ArrayList<>();
        for (Queue<TaskStat> queue : queues) {
            if (queue.isEmpty())
                continue;
            TaskStat task = queue.peek();
            if (task.stage == Stage.READ || task.stage == Stage.WRITE)
                readWriteTasks.add(task);
        }

        return readWriteTasks;
    }

    private List<TaskStat> getMinRuntimeTasks(Queue<TaskStat> queueInCore, CFSSimulationState simulationState, long time) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (queueInCore.isEmpty())
            return new ArrayList<>();

        Class<?> clazz = Class
                    .forName(this.getClass().getPackage().getName() + ".comparator." + comparatorCase.getClassName());
        Constructor<?> ctor = clazz.getConstructor();
        BasicTaskComparator taskComparator = (BasicTaskComparator) ctor.newInstance(new Object[] {});

        List<TaskStat> minRuntimeTasks = new ArrayList<>();
        TaskStat previousTask = null;
        double minRuntime;
        switch (simulationState.blockingPolicy) {
            case NONE:
                minRuntime = queueInCore.peek().virtualRuntime;
                if (simulationState.getMethod() == ScheduleSimulationMethod.PRIORITY_QUEUE) {
                    if (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime)
                        minRuntimeTasks.add(queueInCore.poll());
                } else { // BRUTE_FORCE
                    previousTask = null;
                    while (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime) {
                        TaskStat task = queueInCore.poll();
                        minRuntimeTasks.add(task);
                        if(previousTask != null && taskComparator.compare(task, previousTask) != 0) {
                            break;
                        }
                        previousTask = task;
                    }
                }
                break;
            case READ:
                List<TaskStat> readTasks = new ArrayList<>();
                queueInCore.removeIf(t -> {
                    if (t.stage == Stage.READ && t.task.id != simulationState.blockingTaskId) {
                        readTasks.add(t);
                        return true;
                    }
                    return false;
                });

                if (!queueInCore.isEmpty()) {
                    minRuntime = queueInCore.peek().virtualRuntime;
                    if (simulationState.getMethod() == ScheduleSimulationMethod.PRIORITY_QUEUE) {
                        if (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime)
                            minRuntimeTasks.add(queueInCore.poll());
                    } else { // BRUTE_FORCE
                        previousTask = null;
                        while (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime) {
                            TaskStat task = queueInCore.poll();
                            minRuntimeTasks.add(task);
                            if(previousTask != null && taskComparator.compare(task, previousTask) != 0) {
                            break;
                            }
                            previousTask = task;
                        }
                    }
                }
                addAllIntoQueue(queueInCore, readTasks, time);
                break;
            case WRITE:
                List<TaskStat> writeTasks = new ArrayList<>();
                queueInCore.removeIf(t -> {
                    if (t.stage == Stage.WRITE && t.task.id != simulationState.blockingTaskId) {
                        writeTasks.add(t);
                        return true;
                    }
                    return false;
                });

                if (!queueInCore.isEmpty()) {
                    minRuntime = queueInCore.peek().virtualRuntime;
                    if (simulationState.getMethod() == ScheduleSimulationMethod.PRIORITY_QUEUE) {
                        if (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime)
                            minRuntimeTasks.add(queueInCore.poll());
                    } else { // BRUTE_FORCE
                        previousTask = null;
                        while (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime) {
                            TaskStat task = queueInCore.poll();
                            minRuntimeTasks.add(task);
                            if(previousTask != null && taskComparator.compare(task, previousTask) != 0) {
                                break;
                            }
                            previousTask = task;
                        }
                    }
                }
                addAllIntoQueue(queueInCore, writeTasks, time);
                break;
        }

        return minRuntimeTasks;
    }

    private HashMap<Integer, Long> cloneHashMap(HashMap<Integer, Long> mapToBeCopied) {
        HashMap<Integer, Long> clonedMap = new HashMap<Integer, Long>();

        for (Integer key : mapToBeCopied.keySet()) {
            clonedMap.put(key, Long.valueOf(mapToBeCopied.get(key).longValue()));
        }

        return clonedMap;
    }

    private HashMap<Integer, Long> simulatePathBlocking(List<Core> cores, List<Queue<TaskStat>> queues,
            HashMap<Integer, Long> wcrtMap, CFSSimulationState simulationState, long time, long hyperperiod)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.finer("\n*** Path diverged due to blocking ***");

        CFSSimulationState cloneSimulationState = simulationState.copy();
        List<Queue<TaskStat>> cloneQueues = copyQueues(queues);
        HashMap<Integer, Long> cloneWcrtMap = cloneHashMap(wcrtMap);

        simulationState.blockingPolicyReset = false;
        for (int i = 0; i < cores.size(); i++) {
            Queue<TaskStat> cloneQueue = cloneQueues.get(i);
            CoreState cloneCoreState = cloneSimulationState.coreStates.get(i);

            TaskStat cloneTask = null;
            if (cloneCoreState.isRunning)
                cloneTask = cloneCoreState.currentTask;
            else {
                List<TaskStat> cloneMinRuntimeTasks = getMinRuntimeTasks(cloneQueue, simulationState, time);
                if (simulationState.getMethod() == ScheduleSimulationMethod.PRIORITY_QUEUE) {
                    if (cloneMinRuntimeTasks.size() > 0)
                        cloneTask = cloneMinRuntimeTasks.get(0);
                } else { // BRUTE_FORCE
                    if (cloneMinRuntimeTasks.size() > 1) {
                        pathDivergesEqualMinRuntime(i, cloneMinRuntimeTasks, cores, cloneQueues, cloneWcrtMap,
                                cloneSimulationState, time, hyperperiod);
                        return cloneWcrtMap;
                    }
                    else if (cloneMinRuntimeTasks.size() == 1)
                        cloneTask = cloneMinRuntimeTasks.get(0);
                }
                if (cloneTask == null)
                    continue;
                setRuntime(i, cloneTask, cloneQueue, cloneSimulationState);
                simulationState.putEventTime((time + cloneCoreState.remainingRuntime));
            }
            executeTask(cloneTask, cloneQueue, cloneWcrtMap, cloneSimulationState, cloneCoreState, time, i);
            updateMinimumVirtualRuntime(cloneCoreState, cloneQueue);
        }
        if (cloneSimulationState.blockingPolicyReset)
            cloneSimulationState.blockingPolicy = BlockingPolicy.NONE;

        simulationState.setPreviousEventTime(time);
        time = simulationState.getNextEventTime();
        logger.fine("Time popped out: " + time);

        performSimulation(cores, cloneQueues, cloneWcrtMap, cloneSimulationState, time, hyperperiod, 0);
        checkSchedulability(cores, cloneQueues, cloneWcrtMap);
        return cloneWcrtMap;
    }

    /**
     * 주어진 minRuntimeTask에 대해 runtime을 계산한다.
     * 나머지 코어에 대해서 task를 고르고 coreState에 저장하고 runtime을 계산한다.
     * 나머지 시뮬레이션을 동일하게 진행하고, WCRTs를 업데이트한다.
     * 
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    private SimulationResult simulatePathEqualMinRuntime(List<Core> cores, List<Queue<TaskStat>> queues,
            HashMap<Integer, Long> wcrtMap, CFSSimulationState simulationState, long time, long hyperperiod,
            List<TaskStat> minRuntimeTasks, long scheduleID, int taskIndex, int coreIndex)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.finer("\n*** Path diverged due to equal minimum runtime ***");

        List<TaskStat> cloneMinRuntimeTasks = new ArrayList<>();
        for (TaskStat task : minRuntimeTasks)
            cloneMinRuntimeTasks.add(task.copy());
        TaskStat minRuntimeTask = cloneMinRuntimeTasks.remove(taskIndex);
        List<Core> cloneCores = new ArrayList<>();
        for (Core core : cores) {
            cloneCores.add(core.copy());
        }

        CFSSimulationState cloneSimulationState = simulationState.copy();
        List<Queue<TaskStat>> cloneQueues = copyQueues(queues);
        
        // Since it clones the queue, we must not change the queue insert time
        cloneQueues.get(coreIndex).addAll(cloneMinRuntimeTasks);
        HashMap<Integer, Long> cloneWcrtMap = cloneHashMap(wcrtMap);
        Queue<TaskStat> cloneQueue = cloneQueues.get(coreIndex);
        CoreState cloneCoreState = cloneSimulationState.coreStates.get(coreIndex);

        cloneSimulationState.setSelectedDivergeIndex(taskIndex);
        cloneSimulationState.setSimulationScheduleID(scheduleID);

        logger.fine("Task " + minRuntimeTask.task.id + "(vruntime:" + minRuntimeTask.virtualRuntime + ") started to run at time " + time);
        setRuntime(coreIndex, minRuntimeTask, cloneQueue, cloneSimulationState);
        cloneSimulationState.putEventTime((time + cloneCoreState.remainingRuntime));
        cloneCoreState.currentTask = minRuntimeTask;

        for (int i = coreIndex + 1; i < cloneCores.size(); i++) {
            cloneQueue = cloneQueues.get(i);
            cloneCoreState = cloneSimulationState.coreStates.get(i);
            TaskStat cloneTask = null;
            if (cloneCoreState.isRunning) {
                cloneTask = cloneCoreState.currentTask;
                executeTask(cloneTask, cloneQueue, cloneWcrtMap, cloneSimulationState, cloneCoreState, time, i);
                updateMinimumVirtualRuntime(cloneCoreState, cloneQueue);
            }
            if(cloneCoreState.isRunning == false) {
                cloneTask = null;
                cloneMinRuntimeTasks = getMinRuntimeTasks(cloneQueue, cloneSimulationState, time);
                if (cloneSimulationState.getMethod() == ScheduleSimulationMethod.PRIORITY_QUEUE) {
                    if (cloneMinRuntimeTasks.size() > 0)
                        cloneTask = cloneMinRuntimeTasks.get(0);
                } else { // BRUTE_FORCE
                    if (cloneMinRuntimeTasks.size() > 1) {
                        return pathDivergesEqualMinRuntime(i, cloneMinRuntimeTasks, cores, cloneQueues, cloneWcrtMap,
                                cloneSimulationState, time, hyperperiod);
                    }
                    else if (cloneMinRuntimeTasks.size() == 1)
                        cloneTask = cloneMinRuntimeTasks.get(0);
                }
                if (cloneTask == null)
                    continue;
                else
                    logger.fine("Task " + cloneTask.task.id + "(vruntime:" + cloneTask.virtualRuntime + ") started to run at time " + time);
                setRuntime(i, cloneTask, cloneQueue, cloneSimulationState);
                cloneSimulationState.putEventTime((time + cloneCoreState.remainingRuntime));
                cloneCoreState.currentTask = cloneTask;
            }
            
        }
        if (cloneSimulationState.blockingPolicyReset)
            cloneSimulationState.blockingPolicy = BlockingPolicy.NONE;

        cloneSimulationState.setPreviousEventTime(time);
        time = cloneSimulationState.getNextEventTime();
        //logger.info("time popped up2: " + time);

        performSimulation(cloneCores, cloneQueues, cloneWcrtMap, cloneSimulationState, time, hyperperiod, coreIndex);
        return checkSchedulability(cloneCores, cloneQueues, cloneWcrtMap);
    }

    private List<Queue<TaskStat>> copyQueues(List<Queue<TaskStat>> originalQueues)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<Queue<TaskStat>> newQueues = new ArrayList<>();

        for (Queue<TaskStat> originalQueueInCore : originalQueues) {
            Class<?> clazz = Class
                    .forName(this.getClass().getPackage().getName() + ".comparator." + comparatorCase.getClassName());
            Constructor<?> ctor = clazz.getConstructor();
            BasicTaskComparator taskComparator = (BasicTaskComparator) ctor.newInstance(new Object[] {});
            Queue<TaskStat> newQueueInCore = new PriorityQueue<>(taskComparator);
            // Since it clones the queue, we must not change the queue insert time
            for (TaskStat task : originalQueueInCore) {
                newQueueInCore.add(task.copy());
            }
            newQueues.add(newQueueInCore);
        }

        return newQueues;
    }

    private SimulationResult checkSchedulability(List<Core> cores, List<Queue<TaskStat>> queues,
            HashMap<Integer, Long> wcrtMap) {
        boolean schedulability = true;

        logger.fine("\n------------------------------");
        logger.fine("***** Simulation Results *****");
        logger.fine("------------------------------");

        logger.fine("Unfinished tasks");
        for (int i = 0; i < queues.size(); i++) {
            Queue<TaskStat> queue = queues.get(i);
            logger.fine("- Core " + (i + 1) + ": " + queue.stream().map(task -> task.task.id).collect(Collectors.toList()));
            if (queue.size() > 0)
                schedulability = false;
        }

        for (int i = 0; i < cores.size(); i++) {
            logger.fine("\nCore " + (i + 1) + " Results");
            for (int j = 0; j < cores.get(i).tasks.size(); j++) {
                Task task = cores.get(i).tasks.get(j);
                logger.fine("- Task " + task.id + " (WCRT: " + wcrtMap.get(Integer.valueOf(task.getId())) + ", Period: "
                        + task.period + ")");
                if (wcrtMap.get(Integer.valueOf(task.getId())) > task.period) { // ns
                    schedulability = false;
                }
            }
        }
        return new SimulationResult(schedulability, wcrtMap);
    }

    public Task findTaskbyID(TestConfiguration testConf, int targetTaskID){
        for(Core core : testConf.mappingInfo){
            for(Task task : core.tasks){
                if(task.id == targetTaskID)
                    return task;
            }
        }
        return null;
    }

}
