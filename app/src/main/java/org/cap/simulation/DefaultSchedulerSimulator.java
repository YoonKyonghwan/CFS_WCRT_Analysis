package org.cap.simulation;

import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;
import java.util.List;
import java.util.Stack;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.cap.model.BlockingPolicy;
import org.cap.model.SimulationState;
import org.cap.model.Core;
import org.cap.model.CoreState;
import org.cap.model.RunQueue;
import org.cap.model.ScheduleCache;
import org.cap.model.ScheduleCacheData;
import org.cap.model.SchedulePickResult;
import org.cap.model.ScheduleSimulationMethod;
import org.cap.model.SimulationResult;
import org.cap.model.Stage;
import org.cap.model.Task;
import org.cap.model.TaskStat;
import org.cap.model.TestConfiguration;
import org.cap.simulation.comparator.MultiComparator;
import org.cap.simulation.comparator.ComparatorCase;
import org.cap.utility.LoggerUtility;



public abstract class DefaultSchedulerSimulator {
    private MultiComparator comparator;
    private HashSet<String> finalScheduleHash;
    private boolean initialOrder;

    protected List<ComparatorCase> comparatorCaseList;
    protected static final Logger logger = LoggerUtility.getLogger();
    protected long triedScheduleCount = 0;
    protected ScheduleSimulationMethod method;
    protected long numOfTryToSchedule;
    protected ScheduleCache scheduleCache;
    protected long minimumGranularity = 1 * 1000L;
    protected boolean needsEligibleCheck = false;

    public DefaultSchedulerSimulator(ScheduleSimulationMethod method, long minimumGranularity, long numOfTryToSchedule, boolean initialOrder) {
        this.method = method;
        
        this.comparatorCaseList = null;
        this.initialOrder = initialOrder;
        this.minimumGranularity = minimumGranularity * 1000L;
        this.triedScheduleCount = 0;
        this.scheduleCache = new ScheduleCache(this.method);
        this.comparator = new MultiComparator();
        
        this.finalScheduleHash = new HashSet<String>();
        this.numOfTryToSchedule = numOfTryToSchedule;
    }

    // call this function in child's constructor
    protected List<RunQueue> createRunQueues(int coreNum, List<ComparatorCase> comparatorCaseList, boolean needsEligibleCheck) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<RunQueue> queues = new ArrayList<>();
        for(int i = 0 ; i < coreNum ; i++) {
            RunQueue queueInCore = new RunQueue(comparatorCaseList, needsEligibleCheck);
            queues.add(queueInCore);
        }

        return queues;
    }

    abstract protected TaskStat initializeTaskStat(Task task, int targetTaskID);

    abstract protected TaskStat initializeWakeupTaskStat(Task task, CoreState coreState, RunQueue queue, long time);

    abstract protected long checkTaskAdditionalRuntime(TaskStat task, CoreState coreState, RunQueue queueInCore, SimulationState simulationState, long time);

    abstract protected void updateMinimumVirtualRuntime(CoreState coreState, RunQueue queue);

    abstract protected long getTimeSlice(TaskStat task, RunQueue queueInCore);

    abstract protected TaskStat updateTaskStatAfterRun(TaskStat task, RunQueue queueInCore, long timeUpdated, long remainedTime, SimulationState simulationState);

    public long getTriedScheduleCount() {
        return triedScheduleCount;
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

    private void permutation(ArrayList<ArrayList<Integer>> fullList, Stack<Integer> st, ArrayList<Boolean> check,  ArrayList<Integer> arr, int n, int r) {
		if(st.size() == r){
            ArrayList<Integer> toAdd = new ArrayList<Integer>();
            for(Integer i : st) {
                toAdd.add(Integer.valueOf(i));
            }
            fullList.add(toAdd);
			return;
		}

		for(int i=0; i<n; i++){
			if(!check.get(i)){
				check.set(i, true);
				st.push(arr.get(i));
				permutation(fullList, st, check, arr, n, r);
				st.pop();
                check.set(i, false);
			}
		}
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

    private List<TaskStat> getBlockingTasks(List<RunQueue> queues, SimulationState simulationState) {
        if (simulationState.blockingPolicy != BlockingPolicy.NONE)
            return new ArrayList<>();

        List<TaskStat> readWriteTasks = new ArrayList<>();
        for (RunQueue queue : queues) {
            if (queue.isEmpty())
                continue;
            TaskStat task = queue.peek();
            if (task.stage == Stage.READ || task.stage == Stage.WRITE)
                readWriteTasks.add(task);
        }

        return readWriteTasks;
    }

    public SimulationResult simulate(List<Core> cores, int targetTaskID, long simulationTime)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.fine("\n------------------------------");
        logger.fine("*** CFS Simulation Started ***");
        logger.fine("------------------------------");
        SimulationResult finalSimulationResult;
        List<RunQueue> queues = createRunQueues(cores.size(), this.comparatorCaseList, this.needsEligibleCheck);
        HashMap<Integer, Long> wcrtMap = initializeWCRTs(cores);
        initializeJobs(queues, cores, targetTaskID);
        long time = 0;
        this.triedScheduleCount = 0;
        this.scheduleCache = new ScheduleCache(this.method);

        if(this.method != ScheduleSimulationMethod.RANDOM && this.method != ScheduleSimulationMethod.RANDOM_TARGET_TASK) {
            this.numOfTryToSchedule = 1;
            if (initialOrder == false) {
                SimulationState simulationState = new SimulationState(cores.size());
                simulationState.insertPeriodsAtStartTime(cores);
                simulationState.setPreviousEventTime(time);
                long previousTime = simulationState.getPreviousEventTime();
                time = simulationState.peekNextEventTime();
                simulationState.insertPeriodsInTimeRange(cores, previousTime, time);
                time = simulationState.getNextEventTime();

                finalSimulationResult = performSimulation(cores, queues, wcrtMap, simulationState, time, simulationTime, 0, false);
            } else {
                ArrayList<Integer> initialOrderList = new ArrayList<Integer>();
                Stack<Integer> stack = new Stack<Integer>();
                ArrayList<Boolean> check = new ArrayList<Boolean>();
                ArrayList<ArrayList<Integer>> fullList = new ArrayList<ArrayList<Integer>>();
                for(int  i = 0 ; i < wcrtMap.size() ; i++) {
                    initialOrderList.add(i);
                    check.add(false);
                }
                finalSimulationResult = new SimulationResult();
                permutation(fullList, stack, check, initialOrderList, wcrtMap.size(), wcrtMap.size());
                int i = 0;
                for(ArrayList<Integer> listPriority : fullList) {
                    setInitialOrderToCores(cores, listPriority);
                    initializeJobs(queues, cores, targetTaskID);
                    SimulationState simulationState = new SimulationState(cores.size());
                    simulationState.insertPeriodsAtStartTime(cores);
                    simulationState.setPreviousEventTime(time);
                    long previousTime = simulationState.getPreviousEventTime();
                    time = simulationState.peekNextEventTime();
                    simulationState.insertPeriodsInTimeRange(cores, previousTime, time);
                    time = simulationState.getNextEventTime();
                    SimulationResult simulResult = performSimulation(cores, queues, wcrtMap, simulationState, time, simulationTime, 0, false);
                    finalSimulationResult = mergeToFinalResult(finalSimulationResult, simulResult);   
                    i++;   
                }
                //System.out.println(i);
            }
        } else {
            SimulationState simulationState = new SimulationState(cores.size());
            simulationState.insertPeriodsAtStartTime(cores);
            simulationState.setPreviousEventTime(time);
            long previousTime = simulationState.getPreviousEventTime();
            time = simulationState.peekNextEventTime();
            simulationState.insertPeriodsInTimeRange(cores, previousTime, time);
            time = simulationState.getNextEventTime();
            finalSimulationResult = performSimulation(cores, queues, wcrtMap, simulationState, time, simulationTime, 0, false);
            for(int i = 0 ; i < this.numOfTryToSchedule - 1 ; i++) {
                SchedulePickResult pickResult = this.scheduleCache.pickScheduleData(this.comparator);
                if (pickResult == null) 
                    break;
                ScheduleCacheData pickData = pickResult.getScheduleData();
                pickData.getQueues().get(pickData.getCoreIndex()).addAll(pickData.getEqualPriorityTasks());
                SimulationResult simulResult = performSimulation(cores, pickData.getQueues(), wcrtMap,
                        pickData.getSimulationState(), pickData.getTime(), simulationTime, pickData.getCoreIndex(), true);
                finalSimulationResult = mergeToFinalResult(finalSimulationResult, simulResult);                
            }
        }
        logger.fine("\n------------------------------");
        logger.fine("******** Final Result ********");
        logger.fine("------------------------------");
        return finalSimulationResult;
    }


    private SimulationResult performSimulation(List<Core> cores, List<RunQueue> queues, HashMap<Integer, Long> wcrtMap,
            SimulationState simulationState, long time, long simulationTime, int coreIndex, boolean resume)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        boolean diverged = false;
        ScheduleCacheData scheduleData = null;
        List<TaskStat> candidateTasksToRun = null;
        SimulationResult mergedResult = new SimulationResult(true, wcrtMap);

        HashMap<Integer, Long> cloneWcrtMap = cloneHashMap(wcrtMap);

        do {
            diverged = false;
            outerLoop:
            while (time < simulationTime && time >= 0) {
                logger.log(Level.FINER, "\nTime {0}:", time);
                if (resume == false) {
                    addJobs(cores, queues, simulationState.coreStates, time);

                    List<TaskStat> blockingTasks = getBlockingTasks(queues, simulationState);
                    if (blockingTasks.size() > 1) {
                        String scheduleID = this.scheduleCache.pushScheduleData(simulationState.getSimulationScheduleID(), queues, cloneWcrtMap, simulationState, time, blockingTasks, coreIndex, this.comparator);
                        int taskIndexToSelect = pickNextTaskToSchedule(simulationState, scheduleID);
                        if(taskIndexToSelect == -1) {
                            this.scheduleCache.popScheduleData();
                            diverged = true;
                            break;
                        } else {
                            if (blockingTasks.get(taskIndexToSelect).stage == Stage.READ)
                                simulationState.blockingPolicy = BlockingPolicy.READ;
                            else
                                simulationState.blockingPolicy = BlockingPolicy.WRITE;
                            simulationState.blockingTaskId = blockingTasks.get(taskIndexToSelect).task.id;
                        }
                    }

                    simulationState.blockingPolicyReset = false;
                } else {
                    resume = false;
                }
                
                for (; coreIndex < cores.size(); coreIndex++) {
                    RunQueue queue = queues.get(coreIndex);
                    CoreState coreState = simulationState.coreStates.get(coreIndex);

                    TaskStat task = null;
                    if (coreState.isRunning) {
                        task = coreState.currentTask;
                        executeTask(task, queue, cloneWcrtMap, simulationState, coreState, time, coreIndex);
                        updateMinimumVirtualRuntime(coreState, queue);
                    }
                    if(coreState.isRunning == false) {
                        task = null;
                        candidateTasksToRun = pickNextCandidateTasks(queue, simulationState, coreState.minimumVirtualRuntime, time);
                        if (this.method == ScheduleSimulationMethod.PRIORITY_QUEUE) {
                            if (!candidateTasksToRun.isEmpty()) {
                                task = candidateTasksToRun.get(0);
                                candidateTasksToRun.remove(0);
                                queue.addAll(candidateTasksToRun);
                            }
                        } else { // BRUTE_FORCE or RANDOM or RANDOM_TARGET_TASK
                            if (candidateTasksToRun.size() > 1) {
                                String scheduleID = this.scheduleCache.pushScheduleData(simulationState.getSimulationScheduleID(), queues, cloneWcrtMap, simulationState, time, candidateTasksToRun, coreIndex, this.comparator);
                                int taskIndexToSelect = pickNextTaskToSchedule(simulationState, scheduleID);
                                if(taskIndexToSelect == -1) {
                                    this.scheduleCache.popScheduleData();
                                    diverged = true;
                                    break outerLoop;
                                } else {
                                    task = candidateTasksToRun.get(taskIndexToSelect);
                                    candidateTasksToRun.remove(taskIndexToSelect);
                                    queue.addAll(candidateTasksToRun);
                                }
                            }
                            else if (candidateTasksToRun.size() == 1)
                                task = candidateTasksToRun.get(0);
                        }
                        if (task == null)
                            continue;
                        else {
                            logger.log(Level.FINE, "Task {0}(vruntime:{1}) started to run at time {2} : vd {3}", new Object[]{task.task.id, task.virtualRuntime, time, task.virtualDeadline});
                        }
                        setRuntime(simulationState.coreStates.get(coreIndex), task, queue);
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
                    RunQueue queue = queues.get(coreIndex);
                    queue.clear();
                }
            } else {
                String scheduleHashString = simulationState.getSimulationScheduleID() + "_" + simulationState.getSelectedDivergeIndex();
                if (!this.finalScheduleHash.contains(scheduleHashString) || scheduleHashString.length() <= 3) {
                    this.finalScheduleHash.add(simulationState.getSimulationScheduleID() + "_" + simulationState.getSelectedDivergeIndex());
                    this.triedScheduleCount++;

                    if(this.triedScheduleCount % 10000 == 0) {
                        logger.log(Level.FINE, "tried: {0}", this.triedScheduleCount);
                    }

                    SimulationResult simulResult = checkSchedulability(cores, queues, cloneWcrtMap);
                    mergedResult = mergeToFinalResult(mergedResult, simulResult);
                    if(this.method != ScheduleSimulationMethod.PRIORITY_QUEUE) { 
                        this.scheduleCache.saveFinalScheduledIndex(simulationState.getSimulationScheduleID(), simulationState);
                    }
                } else {
                    logger.log(Level.FINE, "duplicated schedule: {0}", scheduleHashString);
                }
                
                if(this.method == ScheduleSimulationMethod.RANDOM || 
                this.method == ScheduleSimulationMethod.RANDOM_TARGET_TASK) {
                    this.scheduleCache.clearStack();
                    break;
                }
            }
            if(this.scheduleCache.getScheduleStackSize() > 0) {
                scheduleData = this.scheduleCache.popScheduleData().copy(this.comparator);
                queues = scheduleData.getQueues();
                simulationState = scheduleData.getSimulationState();
                time = scheduleData.getTime();
                candidateTasksToRun = scheduleData.getEqualPriorityTasks();
                coreIndex = scheduleData.getCoreIndex();
                queues.get(coreIndex).addAll(candidateTasksToRun);
                resume = true;
            }
        } while(this.scheduleCache.getScheduleStackSize() > 0 || resume == true);

        return mergedResult;
    }

    private HashMap<Integer, Long> cloneHashMap(HashMap<Integer, Long> mapToBeCopied) {
        HashMap<Integer, Long> clonedMap = new HashMap<Integer, Long>();

        for (Integer key : mapToBeCopied.keySet()) {
            clonedMap.put(key, Long.valueOf(mapToBeCopied.get(key).longValue()));
        }

        return clonedMap;
    }

    private SimulationResult checkSchedulability(List<Core> cores, List<RunQueue> queues,
            HashMap<Integer, Long> wcrtMap) {
        boolean schedulability = true;

        logger.fine("\n------------------------------");
        logger.fine("***** Simulation Results *****");
        logger.fine("------------------------------");

        logger.fine("Unfinished tasks");
        for (int i = 0; i < queues.size(); i++) {
            RunQueue queue = queues.get(i);
            logger.log(Level.FINE, "- Core {0}{1}: {2}", new Object[]{i, 1, queue.stream().map(task -> task.task.id).collect(Collectors.toList())});
            if (!queue.isEmpty())
                schedulability = false;
        }

        for (int i = 0; i < cores.size(); i++) {
            logger.fine("\nCore " + (i + 1) + " Results");
            for (int j = 0; j < cores.get(i).tasks.size(); j++) {
                Task task = cores.get(i).tasks.get(j);
                logger.log(Level.FINE, "- Task {0} (WCRT: {1}, Period: {2})", new Object[]{task.id, wcrtMap.get(Integer.valueOf(task.getId())), task.period});
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

    protected void initializeJobs(List<RunQueue> queues, List<Core> cores, int targetTaskID) {
        int index = 0;
        for (Core core : cores) {
            RunQueue queueInCore = queues.get(index);
            queueInCore.clear();
            for (Task task : core.tasks) {
                if (targetTaskID == task.id)
                    task.isTargetTask = true;
                else
                    task.isTargetTask = false;

                TaskStat taskStat = initializeTaskStat(task, targetTaskID);

                if (task.startTime == 0L) {
                    queueInCore.addIntoQueue(taskStat.copy(), 0L);
                }
            }
            index++;
        }
    }

    private List<TaskStat> pickNextCandidateTasks(RunQueue queueInCore, SimulationState simulationState, long minimumVirtualRuntime, long time) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<TaskStat> candidateTasks = new ArrayList<>();
        boolean popAll = true;

        if (queueInCore.isEmpty())
            return candidateTasks;

        if (this.method == ScheduleSimulationMethod.PRIORITY_QUEUE) {
            popAll = false;
        }

        switch (simulationState.blockingPolicy) {
            case NONE:
                candidateTasks = queueInCore.popCandidateTasks(popAll, minimumVirtualRuntime);
                break;
            case READ:
                List<TaskStat> readTasks = queueInCore.removeBlockingTasks(Stage.READ, simulationState.blockingTaskId);
                candidateTasks = queueInCore.popCandidateTasks(popAll, minimumVirtualRuntime);
                queueInCore.addAllIntoQueue(readTasks, time);
                break;
            case WRITE:
                List<TaskStat> writeTasks =  queueInCore.removeBlockingTasks(Stage.WRITE, simulationState.blockingTaskId);
                candidateTasks = queueInCore.popCandidateTasks(popAll, minimumVirtualRuntime);
                queueInCore.addAllIntoQueue(writeTasks, time);
                break;
        }

        return candidateTasks;
    }

    private int pickNextTaskToSchedule(SimulationState simulationState, String scheduleId) {
        SchedulePickResult pickResult;
        if(this.method == ScheduleSimulationMethod.BRUTE_FORCE) {
            pickResult = this.scheduleCache.pickScheduleDataByEntry(scheduleId, false);
        } else { // RANDOM or RANDOM_TARGET_TASK
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

    public List<Core>  setInitialOrderToCores(List<Core> cores, ArrayList<Integer> initialOrderList) {
        int i = 0;
        for (Core core : cores) {
            for(Task task : core.tasks) {
                task.initialPriority = initialOrderList.get(i).intValue();
                i++;
            }
        }

        return cores;
    }

    protected boolean initialJobs(long time, Task task) {
        return task.startTime > 0 && time == task.startTime;
    }

    protected boolean periodicJobs(long time, Task task) {
        return time > task.startTime && task.period > 0 && (time - task.startTime) % task.period == 0;
    }

    protected void skipReadStageIfNoReadTime(TaskStat task) {
        if (task.stage == Stage.READ && task.readTimeInNanoSeconds <= 0) {
            task.stage = Stage.BODY;
            task.bodyReleaseTime = task.readReleaseTime;
        }
    }

    private void addJobs(List<Core> cores, List<RunQueue> queues, List<CoreState> coreStates, long time) {
        for (Core core : cores) {
            CoreState coreState = coreStates.get(core.coreID - 1);
            RunQueue queue = queues.get(core.coreID - 1);
            ArrayList<TaskStat> tasksToPutInQueue = new ArrayList<>();

            for (Task task : core.tasks) {
                if (initialJobs(time, task) || periodicJobs(time, task)) {
                    logger.log(Level.FINE, "Tasks {0} Released at time {1}", new Object[]{task.id, time});
                    // logger.fine("- Task " + task.id + " (Read Time: " + task.readTimeInNanoSeconds + ", Body Time: " + task.bodyTimeInNanoSeconds
                            // + ", Write Time: " + task.writeTimeInNanoSeconds + ")");
                    TaskStat taskStat = initializeWakeupTaskStat(task, coreState, queue, time);
                    tasksToPutInQueue.add(taskStat);
                }
            }

            for (TaskStat task : tasksToPutInQueue) {
                queue.addIntoQueue(task, time);
            }
        }
    }

    protected void executeTask(TaskStat task, RunQueue queueInCore, HashMap<Integer, Long> wcrtMap,
            SimulationState simulationState, CoreState coreState, long time, int coreIndex) {
        logger.log(Level.FINER, "-Core: {0}, Task {1} executed in stage: {2}", new Object[]{coreIndex, task.task.id, task.stage});
        long timeUpdated = time - simulationState.getPreviousEventTime();
        long passedTime = timeUpdated;

        // Decrease runtime
        coreState.remainingRuntime -= timeUpdated;        

        // Decrease execution time for each stage
        while(timeUpdated > 0 && task.stage != Stage.COMPLETED) {
            switch (task.stage) {
                case READ:
                    if(timeUpdated <= task.readTimeInNanoSeconds) {
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
                    logger.log(Level.SEVERE, "Task {0} entered with completed stage.", task.task.id);
                    break;
                default:
                    break;
            }
        }

        // Update virtual runtime
        // long temp = (long) (Math.ceil(((timeUpdated << 10L)  / task.weight) /100) *100);
        // task.virtualRuntime += temp ;
        task = updateTaskStatAfterRun(task, queueInCore, passedTime, coreState.remainingRuntime, simulationState);

        // Add task back to queue if task is not finished but runtime is over
        if (task.stage != Stage.COMPLETED) {
            // handling the preemption from other tasks becuase of new task invocation
            // if time is still remained (which means the task cannot be executed until the time slice), clear the next end event of this task
            coreState.isRunning = false;
            if(coreState.remainingRuntime > 0) { 
                simulationState.clearEventTime(time + coreState.remainingRuntime);
                long remainedTime = checkTaskAdditionalRuntime(task, coreState, queueInCore, simulationState, time);
                if (remainedTime > 0) {
                    coreState.isRunning = true;
                    coreState.remainingRuntime = remainedTime;
                    simulationState.putEventTime(time + remainedTime);
                }
            }

            if(coreState.isRunning == false) {
                queueInCore.addIntoQueue(task, time);
            }

            if (simulationState.blockingPolicy == BlockingPolicy.READ)
                simulationState.blockingPolicyReset = true;
        } else {
            logger.log(Level.FINE, "Task {0} completed at time {1} with RT {2}", new Object[]{task.task.id, time, time - task.readReleaseTime});
            wcrtMap.put(Integer.valueOf(task.getId()),
                    Math.max(wcrtMap.get(Integer.valueOf(task.getId())), time - task.readReleaseTime));
            coreState.putFinishedTaskVirtualRuntime(task.task.id, task.virtualRuntime);
            coreState.isRunning = false;
        }
        
    }

    protected void setRuntime(CoreState coreState, TaskStat task, RunQueue queueInCore) {
        coreState.remainingRuntime = getTimeSlice(task, queueInCore);
        coreState.isRunning = true;
    }

}
