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

    public CFSSimulator(ScheduleSimulationMethod method, ComparatorCase comparatorCase, int targetLatency) {
        this.method = method;
        this.comparatorCase = comparatorCase;
        this.targetLatency = targetLatency * 1000L;
        this.triedScheduleCount = 0;
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

    public SimulationResult simulateCFS(List<Core> cores, int targetTaskID, long simulationTime)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.fine("\n------------------------------");
        logger.fine("*** CFS Simulation Started ***");
        logger.fine("------------------------------");

        HashMap<Integer, Long> wcrtMap = initializeWCRTs(cores);
        List<Queue<Task>> queues = initializeQueues(cores, targetTaskID);
        CFSSimulationState simulationState = new CFSSimulationState(this.targetLatency, this.minimumGranularity,
                cores.size(), this.method);
        long time = 0;

        if(simulationTime == 0) { // hyper period
            simulationTime = MathUtility.getLCM(cores);
        }
        else if (simulationTime == -1) {
            simulationTime = getMaximumPeriod(cores);
        }

        simulationState.insertPeriodsIntoEventQueue(simulationTime, cores);
        simulationState.setPreviousEventTime(time);
        time = simulationState.getNextEventTime();

        performSimulation(cores, queues, wcrtMap, simulationState, time, simulationTime);

        logger.fine("\n------------------------------");
        logger.fine("******** Final Result ********");
        logger.fine("------------------------------");
        return checkSchedulability(cores, queues, wcrtMap);
    }

    private void updateOriginalTaskStructure(List<Core> cores, Task task) {
        for(Core core : cores) {
            for(Task taskInCore :core.tasks) {
                if(taskInCore.id == task.id)
                    taskInCore.virtualRuntime = task.virtualRuntime;
            }
        }
    }

    private void performSimulation(List<Core> cores, List<Queue<Task>> queues, HashMap<Integer, Long> wcrtMap,
            CFSSimulationState simulationState, long time, long simulationTime)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        boolean diverged = false;
        int i = 0;

        outerLoop:
        while (time < simulationTime && time >= 0) {
            logger.finer("\nTime " + time + ":");
            addJobs(cores, queues, simulationState.coreStates, time);

            List<Task> blockingTasks = getBlockingTasks(queues, simulationState);
            if (blockingTasks.size() > 1) {
                pathDivergesBlocking(blockingTasks, cores, queues, wcrtMap, simulationState, time, simulationTime);
                break outerLoop;
            }

            simulationState.blockingPolicyReset = false;
            for (i = 0; i < cores.size(); i++) {
                Queue<Task> queue = queues.get(i);
                CoreState coreState = simulationState.coreStates.get(i);

                Task task = null;
                if (coreState.isRunning) {
                    task = coreState.currentTask;
                    executeTask(task, queue, wcrtMap, simulationState, coreState, time, i);
                    updateMinimumVirtualRuntime(coreState, queue);
                    if(queue.size() == 0) {
                        coreState.minimumVirtualRuntime = task.virtualRuntime;
                    }
                    updateOriginalTaskStructure(cores, task);
                }
                if(coreState.isRunning == false) {
                    task = null;
                    List<Task> minRuntimeTasks = getMinRuntimeTasks(queue, simulationState);
                    if (simulationState.getMethod() == ScheduleSimulationMethod.PRIORITY_QUEUE) {
                        if (minRuntimeTasks.size() > 0)
                            task = minRuntimeTasks.get(0);
                    } else { // BRUTE_FORCE
                        if (minRuntimeTasks.size() > 1) {
                            pathDivergesEqualMinRuntime(i, minRuntimeTasks, cores, queues, wcrtMap, simulationState, time, simulationTime);
                            diverged = true;
                            break outerLoop;
                        }
                        else if (minRuntimeTasks.size() == 1)
                            task = minRuntimeTasks.get(0);
                    }
                    if (task == null)
                        continue;
                    else
                        logger.fine("Task " + task.id + "(vruntime:" + task.virtualRuntime + ") started to run at time " + time);
                    setRuntime(i, task, queue, simulationState);
                    simulationState.putEventTime((time + coreState.remainingRuntime));
                    coreState.currentTask = task;
                }
                
            }
            if (simulationState.blockingPolicyReset)
                simulationState.blockingPolicy = BlockingPolicy.NONE;

            simulationState.setPreviousEventTime(time);
            time = simulationState.getNextEventTime();
            //logger.info("time popped up: " + time);
        }

        if (diverged == true) {
            for (i = i + 1; i < cores.size(); i++) {
                Queue<Task> queue = queues.get(i);
                queue.clear();
            }
        } else {
            this.triedScheduleCount++; 
        }
    }

    private void executeTask(Task task, Queue<Task> queueInCore, HashMap<Integer, Long> wcrtMap,
            CFSSimulationState simulationState, CoreState coreState, long time, int coreIndex) {
        logger.finer("-Core: " + coreIndex + ", Task " + task.id + " executed in stage: " + task.stage);
        long timeUpdated = time - simulationState.getPreviousEventTime();
        // Decrease runtime
        
        coreState.remainingRuntime -= timeUpdated;

        // Update virtual runtime
        // long temp = (long) (Math.ceil(((timeUpdated << 10L)  / task.weight) /100) *100);
        // task.virtualRuntime += temp ;

        // long vruntime_increment = (timeUpdated << 10L)  / task.weight ;
        long vruntime_increment = (timeUpdated << 10L) * NiceToWeight.getWeightMul(task.nice) >> 32;
        task.virtualRuntime += vruntime_increment;
        logger.fine("Task " + task.id + " spends " + timeUpdated + " ns [vruntime_increment: " + vruntime_increment + "]");

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
                    logger.severe("Task " + task.id + " entered with completed stage.");
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
            queueInCore.add(task);
            if (simulationState.blockingPolicy == BlockingPolicy.READ)
                simulationState.blockingPolicyReset = true;
        } else {
            logger.fine("Task " + task.id + " completed at time " + time + " with RT "
                    + (time - task.readReleaseTime));
            wcrtMap.put(Integer.valueOf(task.getId()),
                    Math.max(wcrtMap.get(Integer.valueOf(task.getId())), time - task.readReleaseTime));
        }
        coreState.isRunning = false;
    }

    private void updateMinimumVirtualRuntime(CoreState coreState, Queue<Task> queue) {
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

    private List<Queue<Task>> initializeQueues(List<Core> cores, int targetTaskID)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<Queue<Task>> queues = new ArrayList<>();
        for (Core core : cores) {
            Class<?> clazz = Class
                    .forName(this.getClass().getPackage().getName() + ".comparator." + comparatorCase.getClassName());
            Constructor<?> ctor = clazz.getConstructor();
            BasicTaskComparator taskComparator = (BasicTaskComparator) ctor.newInstance(new Object[] {});

            Queue<Task> queueInCore = new PriorityQueue<>(taskComparator);
            for (Task task : core.tasks) {
                task.weight = NiceToWeight.getWeight(task.nice);
                task.originalReadTime = task.readTimeInNanoSeconds;
                task.originalBodyTime = task.bodyTimeInNanoSeconds;
                task.originalWriteTime = task.writeTimeInNanoSeconds;
                task.readReleaseTime = task.startTime;
                task.virtualRuntime = 0L;
                if (targetTaskID == task.id)
                    task.isTargetTask = true;
                else
                    task.isTargetTask = false;
                skipReadStageIfNoReadTime(task);
                if (task.startTime == 0L)
                    queueInCore.add(task.copy());
            }
            queues.add(queueInCore);
        }
        return queues;
    }

    private void addJobs(List<Core> cores, List<Queue<Task>> queues, List<CoreState> coreStates, long time) {
        boolean isAdded = false;
        for (Core core : cores) {
            CoreState coreState = coreStates.get(core.coreID - 1);
            Queue<Task> queue = queues.get(core.coreID - 1);

            for (Task task : core.tasks) {
                if (!isAdded && (initialJobs(time, task) || periodicJobs(time, task))) {
                    logger.fine("\nTasks Released (" + time + "):");
                    isAdded = true;
                }

                if (initialJobs(time, task) || periodicJobs(time, task)) {
                    logger.fine("- Task " + task.id + " (Read Time: " + task.readTimeInNanoSeconds + ", Body Time: " + task.bodyTimeInNanoSeconds
                            + ", Write Time: " + task.writeTimeInNanoSeconds + ")");
                    task.readReleaseTime = time;
                    task.virtualRuntime = Math.max(task.virtualRuntime, coreState.minimumVirtualRuntime);
                    skipReadStageIfNoReadTime(task);
                    queue.add(task.copy());
                }
            }
        }
        // if (isAdded)
        // logger.info("");
    }

    private boolean initialJobs(long time, Task task) {
        return task.startTime > 0 && time == task.startTime;
    }

    private boolean periodicJobs(long time, Task task) {
        return time > task.startTime && task.period > 0 && (time - task.startTime) % task.period == 0;
    }

    private void skipReadStageIfNoReadTime(Task task) {
        if (task.stage == Stage.READ && task.readTimeInNanoSeconds <= 0) {
            task.stage = Stage.BODY;
            task.bodyReleaseTime = task.readReleaseTime;
        }
    }

    private void pathDivergesBlocking(List<Task> blockingTasks, List<Core> cores, List<Queue<Task>> queues,
            HashMap<Integer, Long> wcrtMap, CFSSimulationState simulationState, long time, long hyperperiod)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<HashMap<Integer, Long>> possibleWCRTs = new ArrayList<>();
        for (int i = 0; i < blockingTasks.size(); i++) {
            if (blockingTasks.get(i).stage == Stage.READ)
                simulationState.blockingPolicy = BlockingPolicy.READ;
            else
                simulationState.blockingPolicy = BlockingPolicy.WRITE;

            simulationState.blockingTaskId = blockingTasks.get(i).id;
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

    private void pathDivergesEqualMinRuntime(int coreIndex, List<Task> minRuntimeTasks, List<Core> cores,
            List<Queue<Task>> queues, HashMap<Integer, Long> wcrtMap, CFSSimulationState simulationState, long time,
            long hyperperiod) throws ClassNotFoundException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<HashMap<Integer, Long>> possibleWCRTs = new ArrayList<>();
        for (int i = 0; i < minRuntimeTasks.size(); i++)
            possibleWCRTs.add(simulatePathEqualMinRuntime(cores, queues, wcrtMap, simulationState, time, hyperperiod,
                    minRuntimeTasks, i, coreIndex));

        for (Entry<Integer, Long> entry : wcrtMap.entrySet()) {
            long maxWCRT = 0;
            for (HashMap<Integer, Long> possibleWcrtMap : possibleWCRTs) {
                maxWCRT = Math.max(maxWCRT, possibleWcrtMap.get(entry.getKey()).longValue());
            }
            wcrtMap.put(entry.getKey(), maxWCRT);
        }
    }

    private void setRuntime(int coreIndex, Task task, Queue<Task> queueInCore, CFSSimulationState simulationState) {
        CoreState coreState = simulationState.coreStates.get(coreIndex);
        long totalWeight = queueInCore.stream().mapToLong(t -> t.weight).sum() + task.weight;
        coreState.remainingRuntime = (int) Math.max(simulationState.targetedLatency * task.weight / totalWeight,
                simulationState.minimumGranularity);
        coreState.remainingRuntime = Math.min(coreState.remainingRuntime, (int) (task.readTimeInNanoSeconds + task.bodyTimeInNanoSeconds + task.writeTimeInNanoSeconds));
        coreState.isRunning = true;
    }

    private List<Task> getBlockingTasks(List<Queue<Task>> queues, CFSSimulationState simulationState) {
        if (simulationState.blockingPolicy != BlockingPolicy.NONE)
            return new ArrayList<>();

        List<Task> readWriteTasks = new ArrayList<>();
        for (Queue<Task> queue : queues) {
            if (queue.isEmpty())
                continue;
            Task task = queue.peek();
            if (task.stage == Stage.READ || task.stage == Stage.WRITE)
                readWriteTasks.add(task);
        }

        return readWriteTasks;
    }

    private static List<Task> getMinRuntimeTasks(Queue<Task> queueInCore, CFSSimulationState simulationState) {
        if (queueInCore.isEmpty())
            return new ArrayList<>();

        List<Task> minRuntimeTasks = new ArrayList<>();
        double minRuntime;
        switch (simulationState.blockingPolicy) {
            case NONE:
                minRuntime = queueInCore.peek().virtualRuntime;
                if (simulationState.getMethod() == ScheduleSimulationMethod.PRIORITY_QUEUE) {
                    if (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime)
                        minRuntimeTasks.add(queueInCore.poll());
                } else { // BRUTE_FORCE
                    while (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime)
                        minRuntimeTasks.add(queueInCore.poll());
                }
                break;
            case READ:
                List<Task> readTasks = new ArrayList<>();
                queueInCore.removeIf(t -> {
                    if (t.stage == Stage.READ && t.id != simulationState.blockingTaskId) {
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
                        while (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime)
                            minRuntimeTasks.add(queueInCore.poll());
                    }
                }
                queueInCore.addAll(readTasks);
                break;
            case WRITE:
                List<Task> writeTasks = new ArrayList<>();
                queueInCore.removeIf(t -> {
                    if (t.stage == Stage.WRITE && t.id != simulationState.blockingTaskId) {
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
                        while (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime)
                            minRuntimeTasks.add(queueInCore.poll());
                    }
                }
                queueInCore.addAll(writeTasks);
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

    private HashMap<Integer, Long> simulatePathBlocking(List<Core> cores, List<Queue<Task>> queues,
            HashMap<Integer, Long> wcrtMap, CFSSimulationState simulationState, long time, long hyperperiod)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.info("\n*** Path diverged due to blocking ***");

        CFSSimulationState cloneSimulationState = simulationState.copy();
        List<Queue<Task>> cloneQueues = copyQueues(queues);
        HashMap<Integer, Long> cloneWcrtMap = cloneHashMap(wcrtMap);

        simulationState.blockingPolicyReset = false;
        for (int i = 0; i < cores.size(); i++) {
            Queue<Task> cloneQueue = cloneQueues.get(i);
            CoreState cloneCoreState = cloneSimulationState.coreStates.get(i);

            Task cloneTask = null;
            if (cloneCoreState.isRunning)
                cloneTask = cloneCoreState.currentTask;
            else {
                List<Task> cloneMinRuntimeTasks = getMinRuntimeTasks(cloneQueue, simulationState);
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

        performSimulation(cores, cloneQueues, cloneWcrtMap, cloneSimulationState, time, hyperperiod);
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
    private HashMap<Integer, Long> simulatePathEqualMinRuntime(List<Core> cores, List<Queue<Task>> queues,
            HashMap<Integer, Long> wcrtMap, CFSSimulationState simulationState, long time, long hyperperiod,
            List<Task> minRuntimeTasks, int taskIndex, int coreIndex)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.fine("\n*** Path diverged due to equal minimum runtime ***");

        List<Task> cloneMinRuntimeTasks = new ArrayList<>();
        for (Task task : minRuntimeTasks)
            cloneMinRuntimeTasks.add(task.copy());
        Task minRuntimeTask = cloneMinRuntimeTasks.remove(taskIndex);

        CFSSimulationState cloneSimulationState = simulationState.copy();
        List<Queue<Task>> cloneQueues = copyQueues(queues);
        cloneQueues.get(coreIndex).addAll(cloneMinRuntimeTasks);
        HashMap<Integer, Long> cloneWcrtMap = cloneHashMap(wcrtMap);
        Queue<Task> cloneQueue = cloneQueues.get(coreIndex);
        CoreState cloneCoreState = cloneSimulationState.coreStates.get(coreIndex);

        setRuntime(coreIndex, minRuntimeTask, cloneQueue, cloneSimulationState);
        cloneSimulationState.putEventTime((time + cloneCoreState.remainingRuntime));
        cloneCoreState.currentTask = minRuntimeTask;

        for (int i = coreIndex + 1; i < cores.size(); i++) {
            cloneQueue = cloneQueues.get(i);
            cloneCoreState = cloneSimulationState.coreStates.get(i);
            Task cloneTask = null;
            if (cloneCoreState.isRunning) {
                cloneTask = cloneCoreState.currentTask;
                executeTask(cloneTask, cloneQueue, cloneWcrtMap, cloneSimulationState, cloneCoreState, time, i);
                updateMinimumVirtualRuntime(cloneCoreState, cloneQueue);
            }
            if(cloneCoreState.isRunning == false) {
                cloneTask = null;
                cloneMinRuntimeTasks = getMinRuntimeTasks(cloneQueue, cloneSimulationState);
                if (cloneSimulationState.getMethod() == ScheduleSimulationMethod.PRIORITY_QUEUE) {
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
                else
                    logger.fine("Task " + cloneTask.id + " started to run at time " + time + ", minimum_vruntime: " + cloneTask.virtualRuntime);
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

        performSimulation(cores, cloneQueues, cloneWcrtMap, cloneSimulationState, time, hyperperiod);
        checkSchedulability(cores, cloneQueues, cloneWcrtMap);
        return cloneWcrtMap;
    }

    private List<Queue<Task>> copyQueues(List<Queue<Task>> originalQueues)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<Queue<Task>> newQueues = new ArrayList<>();

        for (Queue<Task> originalQueueInCore : originalQueues) {
            Class<?> clazz = Class
                    .forName(this.getClass().getPackage().getName() + ".comparator." + comparatorCase.getClassName());
            Constructor<?> ctor = clazz.getConstructor();
            BasicTaskComparator taskComparator = (BasicTaskComparator) ctor.newInstance(new Object[] {});
            Queue<Task> newQueueInCore = new PriorityQueue<>(taskComparator);
            for (Task task : originalQueueInCore) {
                newQueueInCore.add(task.copy());
            }
            newQueues.add(newQueueInCore);
        }

        return newQueues;
    }

    private SimulationResult checkSchedulability(List<Core> cores, List<Queue<Task>> queues,
            HashMap<Integer, Long> wcrtMap) {
        boolean schedulability = true;

        logger.fine("\n------------------------------");
        logger.fine("***** Simulation Results *****");
        logger.fine("------------------------------");

        logger.fine("Unfinished tasks");
        for (int i = 0; i < queues.size(); i++) {
            Queue<Task> queue = queues.get(i);
            logger.fine("- Core " + (i + 1) + ": " + queue.stream().map(task -> task.id).collect(Collectors.toList()));
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
                }else{
                    schedulability = true;
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
