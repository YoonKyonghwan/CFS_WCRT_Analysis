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
    private int targetLatency;
    private int minimumGranularity = 1;

    public CFSSimulator(ScheduleSimulationMethod method, ComparatorCase comparatorCase, int targetLatency) {
        this.method = method;
        this.comparatorCase = comparatorCase;
        this.targetLatency = targetLatency;
    }

    public SimulationResult simulateCFS(List<Core> cores, int targetTaskID)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.fine("\n------------------------------");
        logger.fine("*** CFS Simulation Started ***");
        logger.fine("------------------------------");

        HashMap<Integer, Double> wcrtMap = initializeWCRTs(cores);
        List<Queue<Task>> queues = initializeQueues(cores, targetTaskID);
        CFSSimulationState simulationState = new CFSSimulationState(this.targetLatency, this.minimumGranularity,
                cores.size(), this.method);
        int time = 0;
        long hyperperiod = MathUtility.getLCM(cores);

        performSimulation(cores, queues, wcrtMap, simulationState, time, hyperperiod);

        logger.fine("\n------------------------------");
        logger.fine("******** Final Result ********");
        logger.fine("------------------------------");
        return checkSchedulability(cores, queues, wcrtMap);
    }

    private void performSimulation(List<Core> cores, List<Queue<Task>> queues, HashMap<Integer, Double> wcrtMap,
            CFSSimulationState simulationState, int time, long hyperperiod)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        boolean diverged = false;
        int i = 0;

        long max_period = 0;
        for (Core core : cores) {
            for (Task task : core.tasks) {
                if (task.period > max_period)
                    max_period = task.period;
            }
        }

        outerLoop:
        while (time < max_period) {
            logger.finer("\nTime " + time + ":");
            addJobs(cores, queues, simulationState.coreStates, time);

            List<Task> blockingTasks = getBlockingTasks(queues, simulationState);
            if (blockingTasks.size() > 1) {
                pathDivergesBlocking(blockingTasks, cores, queues, wcrtMap, simulationState, time, hyperperiod);
                break outerLoop;
            }

            simulationState.blockingPolicyReset = false;
            for (i = 0; i < cores.size(); i++) {
                Queue<Task> queue = queues.get(i);
                CoreState coreState = simulationState.coreStates.get(i);

                Task task = null;
                if (coreState.isRunning)
                    task = coreState.currentTask;
                else {
                    List<Task> minRuntimeTasks = getMinRuntimeTasks(queue, simulationState);
                    if (simulationState.getMethod() == ScheduleSimulationMethod.PRIORITY_QUEUE) {
                        if (minRuntimeTasks.size() > 0)
                            task = minRuntimeTasks.get(0);
                    } else { // BRUTE_FORCE
                        if (minRuntimeTasks.size() > 1) {
                            pathDivergesEqualMinRuntime(i, minRuntimeTasks, cores, queues, wcrtMap, simulationState, time, hyperperiod);
                            diverged = true;
                            break outerLoop;
                        }
                        else if (minRuntimeTasks.size() == 1)
                            task = minRuntimeTasks.get(0);
                    }
                    if (task == null)
                        continue;
                    else
                         logger.fine("Task " + task.id + " started to run at time " + time + ", minimum_vruntime: " + task.virtualRuntime);
                    setRuntime(i, task, queue, simulationState);
                }
                executeTask(task, queue, wcrtMap, simulationState, coreState, time, i);
                updateMinimumVirtualRuntime(coreState, queue);
            }
            if (simulationState.blockingPolicyReset)
                simulationState.blockingPolicy = BlockingPolicy.NONE;

            time++;
        }

        if (diverged == true) {
            for (i = i + 1; i < cores.size(); i++) {
                Queue<Task> queue = queues.get(i);
                queue.clear();
            }
        }
    }

    private void executeTask(Task task, Queue<Task> queueInCore, HashMap<Integer, Double> wcrtMap,
            CFSSimulationState simulationState, CoreState coreState, int time, int coreIndex) {
        logger.finer("-Core: " + coreIndex + ", Task " + task.id + " executed in stage: " + task.stage);

        // Decrease runtime
        coreState.remainingRuntime--;
        if (coreState.remainingRuntime <= 0)
            coreState.isRunning = false;

        // Update virtual runtime
        task.virtualRuntime += 1024 / task.weight;

        // Decrease execution time for each stage
        switch (task.stage) {
            case READ:
                task.readTime--;
                if (task.readTime <= 0) {
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
                task.bodyTime--;
                if (task.bodyTime <= 0) {
                    if (task.writeTime > 0) {
                        task.stage = Stage.WRITE;
                        task.writeReleaseTime = time + 1;
                    }
                    else {
                        task.stage = Stage.COMPLETED;
                        coreState.isRunning = false;
                    }
                }
                break;
            case WRITE:
                task.writeTime--;
                if (task.writeTime <= 0) {
                    task.stage = Stage.COMPLETED;
                    coreState.isRunning = false;
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

        // Add task back to queue if task is not finished but runtime is over
        if (task.stage != Stage.COMPLETED) {
            if (coreState.isRunning)
                coreState.currentTask = task;
            else {
                queueInCore.add(task);
                if (simulationState.blockingPolicy == BlockingPolicy.READ)
                    simulationState.blockingPolicyReset = true;
            }
        } else {
            logger.fine("Task " + task.id + " completed at time " + (time + 1) + " with RT "
                    + (time - task.readReleaseTime + 1));
            wcrtMap.put(Integer.valueOf(task.getId()),
                    Math.max(wcrtMap.get(Integer.valueOf(task.getId())), time - task.readReleaseTime + 1));
        }
    }

    private void updateMinimumVirtualRuntime(CoreState coreState, Queue<Task> queue) {
        if (queue.size() >= 1)
            coreState.minimumVirtualRuntime = queue.peek().virtualRuntime;
    }

    private HashMap<Integer, Double> initializeWCRTs(List<Core> cores) {
        HashMap<Integer, Double> wcrtMap = new HashMap<Integer, Double>();

        for (Core core : cores) {
            for (Task task : core.tasks) {
                wcrtMap.put(Integer.valueOf(task.getId()), 0.0);
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
                task.originalReadTime = task.readTime;
                task.originalBodyTime = task.bodyTime;
                task.originalWriteTime = task.writeTime;
                task.readReleaseTime = task.startTime;
                task.virtualRuntime = 0;
                if (targetTaskID == task.id)
                    task.isTargetTask = true;
                else
                    task.isTargetTask = false;
                skipReadStageIfNoReadTime(task);
                if (task.startTime == 0)
                    queueInCore.add(task.copy());
            }
            queues.add(queueInCore);
        }
        return queues;
    }

    private void addJobs(List<Core> cores, List<Queue<Task>> queues, List<CoreState> coreStates, int time) {
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
                    logger.fine("- Task " + task.id + " (Read Time: " + task.readTime + ", Body Time: " + task.bodyTime
                            + ", Write Time: " + task.writeTime + ")");
                    task.readReleaseTime = time;
                    task.virtualRuntime = coreState.minimumVirtualRuntime;
                    skipReadStageIfNoReadTime(task);
                    queue.add(task.copy());
                }
            }
        }
        // if (isAdded)
        // logger.info("");
    }

    private boolean initialJobs(int time, Task task) {
        return task.startTime > 0 && time == task.startTime;
    }

    private boolean periodicJobs(int time, Task task) {
        return time > task.startTime && task.period > 0 && (time - task.startTime) % task.period == 0;
    }

    private void skipReadStageIfNoReadTime(Task task) {
        if (task.stage == Stage.READ && task.readTime <= 0) {
            task.stage = Stage.BODY;
            task.bodyReleaseTime = task.readReleaseTime;
        }
    }

    private void pathDivergesBlocking(List<Task> blockingTasks, List<Core> cores, List<Queue<Task>> queues,
            HashMap<Integer, Double> wcrtMap, CFSSimulationState simulationState, int time, long hyperperiod)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<HashMap<Integer, Double>> possibleWCRTs = new ArrayList<>();
        for (int i = 0; i < blockingTasks.size(); i++) {
            if (blockingTasks.get(i).stage == Stage.READ)
                simulationState.blockingPolicy = BlockingPolicy.READ;
            else
                simulationState.blockingPolicy = BlockingPolicy.WRITE;

            simulationState.blockingTaskId = blockingTasks.get(i).id;
            possibleWCRTs.add(simulatePathBlocking(cores, queues, wcrtMap, simulationState, time, hyperperiod));
        }

        for (Entry<Integer, Double> entry : wcrtMap.entrySet()) {
            double maxWCRT = 0;
            for (HashMap<Integer, Double> possibleWcrtMap : possibleWCRTs) {
                maxWCRT = Math.max(maxWCRT, possibleWcrtMap.get(entry.getKey()));
            }
            wcrtMap.put(entry.getKey(), maxWCRT);
        }
    }

    private void pathDivergesEqualMinRuntime(int coreIndex, List<Task> minRuntimeTasks, List<Core> cores,
            List<Queue<Task>> queues, HashMap<Integer, Double> wcrtMap, CFSSimulationState simulationState, int time,
            long hyperperiod) throws ClassNotFoundException, NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<HashMap<Integer, Double>> possibleWCRTs = new ArrayList<>();
        for (int i = 0; i < minRuntimeTasks.size(); i++)
            possibleWCRTs.add(simulatePathEqualMinRuntime(cores, queues, wcrtMap, simulationState, time, hyperperiod,
                    minRuntimeTasks, i, coreIndex));

        for (Entry<Integer, Double> entry : wcrtMap.entrySet()) {
            double maxWCRT = 0;
            for (HashMap<Integer, Double> possibleWcrtMap : possibleWCRTs) {
                maxWCRT = Math.max(maxWCRT, possibleWcrtMap.get(entry.getKey()));
            }
            wcrtMap.put(entry.getKey(), maxWCRT);
        }
    }

    private void setRuntime(int coreIndex, Task task, Queue<Task> queueInCore, CFSSimulationState simulationState) {
        CoreState coreState = simulationState.coreStates.get(coreIndex);
        double totalWeight = queueInCore.stream().mapToDouble(t -> t.weight).sum() + task.weight;
        coreState.remainingRuntime = (int) Math.max(simulationState.targetedLatency * task.weight / totalWeight,
                simulationState.minimumGranularity);
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

    private HashMap<Integer, Double> cloneHashMap(HashMap<Integer, Double> mapToBeCopied) {
        HashMap<Integer, Double> clonedMap = new HashMap<Integer, Double>();

        for (Integer key : mapToBeCopied.keySet()) {
            clonedMap.put(key, Double.valueOf(mapToBeCopied.get(key).doubleValue()));
        }

        return clonedMap;
    }

    private HashMap<Integer, Double> simulatePathBlocking(List<Core> cores, List<Queue<Task>> queues,
            HashMap<Integer, Double> wcrtMap, CFSSimulationState simulationState, int time, long hyperperiod)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.info("\n*** Path diverged due to blocking ***");

        CFSSimulationState cloneSimulationState = simulationState.copy();
        List<Queue<Task>> cloneQueues = copyQueues(queues);
        HashMap<Integer, Double> cloneWcrtMap = cloneHashMap(wcrtMap);

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
            }
            executeTask(cloneTask, cloneQueue, cloneWcrtMap, cloneSimulationState, cloneCoreState, time, i);
            updateMinimumVirtualRuntime(cloneCoreState, cloneQueue);
        }
        if (cloneSimulationState.blockingPolicyReset)
            cloneSimulationState.blockingPolicy = BlockingPolicy.NONE;

        time++;

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
    private HashMap<Integer, Double> simulatePathEqualMinRuntime(List<Core> cores, List<Queue<Task>> queues,
            HashMap<Integer, Double> wcrtMap, CFSSimulationState simulationState, int time, long hyperperiod,
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
        HashMap<Integer, Double> cloneWcrtMap = cloneHashMap(wcrtMap);
        Queue<Task> cloneQueue = cloneQueues.get(coreIndex);
        CoreState cloneCoreState = cloneSimulationState.coreStates.get(coreIndex);

        setRuntime(coreIndex, minRuntimeTask, cloneQueue, cloneSimulationState);
        executeTask(minRuntimeTask, cloneQueue, cloneWcrtMap, cloneSimulationState, cloneCoreState, time, coreIndex);
        for (int i = coreIndex + 1; i < cores.size(); i++) {
            cloneQueue = cloneQueues.get(i);
            cloneCoreState = cloneSimulationState.coreStates.get(i);
            Task cloneTask = null;
            if (cloneCoreState.isRunning)
                cloneTask = cloneCoreState.currentTask;
            else {
                cloneMinRuntimeTasks = getMinRuntimeTasks(cloneQueue, simulationState);
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
            }
            executeTask(cloneTask, cloneQueue, cloneWcrtMap, cloneSimulationState, cloneCoreState, time, i);
            updateMinimumVirtualRuntime(cloneCoreState, cloneQueue);
        }
        if (simulationState.blockingPolicyReset)
            simulationState.blockingPolicy = BlockingPolicy.NONE;

        time++;

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
            HashMap<Integer, Double> wcrtMap) {
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
                if (wcrtMap.get(Integer.valueOf(task.getId())) > task.period) {
                    schedulability = false;
                    task.isSchedulable_by_simulator = false;
                } else {
                    task.isSchedulable_by_simulator = true;
                }
                task.WCRT_by_simulator = (int) Math.floor(wcrtMap.get(Integer.valueOf(task.getId())));
            }
        }
        return new SimulationResult(schedulability, wcrtMap);
    }

}
