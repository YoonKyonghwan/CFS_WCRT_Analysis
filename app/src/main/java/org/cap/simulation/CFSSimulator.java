package org.cap.simulation;

import org.cap.model.*;
import org.cap.simulation.comparator.BasicTaskComparator;
import org.cap.simulation.comparator.ComparatorCase;
import org.cap.utility.LoggerUtility;
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
    private long schedulePeriod; 
    private long targetLatency;
    private long minimumGranularity = 1 * 1000L;
    private long wakeupGranularity = 3 * 1000 * 1000L;
    private long triedScheduleCount = 0;
    private ScheduleCache scheduleCache;
    private long numOfTryToSchedule;
    private BasicTaskComparator comparator;
    private HashSet<String> finalScheduleHash;
    private ArrayList<InterferenceRange> interferenceRangeList;
    private int maximumInterferenceTaskNum;
    private boolean initialOrder;

    public CFSSimulator(ScheduleSimulationMethod method, ComparatorCase comparatorCase, int targetLatency,
            int minimumGranularity, int wakeupGranularity, long numOfTryToSchedule, boolean initialOrder, int scheduling_tick_us)
            throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        this.method = method;
        this.comparatorCase = comparatorCase;
        this.initialOrder = initialOrder;
        this.schedulePeriod = scheduling_tick_us * 1000L;

        Class<?> clazz = Class
                    .forName(ComparatorCase.class.getPackageName() + "." + comparatorCase.getClassName());
        Constructor<?> ctor = clazz.getConstructor();
        this.comparator = (BasicTaskComparator) ctor.newInstance(new Object[] {});
        this.targetLatency = targetLatency * 1000L;
        this.minimumGranularity = minimumGranularity * 1000L;
        this.wakeupGranularity = wakeupGranularity * 1000L;
        this.triedScheduleCount = 0;
        this.scheduleCache = new ScheduleCache(this.method);
        this.numOfTryToSchedule = numOfTryToSchedule;
        this.finalScheduleHash = new HashSet<String>();
        this.interferenceRangeList = new ArrayList<InterferenceRange>();
        this.maximumInterferenceTaskNum = 0;
    }
    
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

    private void makeInterferenceRangeList(List<Core> cores, long simulationTime) {
        PriorityQueue<Long> eventQueue = new PriorityQueue<Long>();
        HashMap<Long, List<Task>> eventTimeMap = new HashMap<Long, List<Task>>();
        for(Core core : cores) {
            for (Task task : core.tasks) {
                long time = task.startTime;
                while(time < simulationTime) {
                    Long timeLong = Long.valueOf(time);
                    if(eventTimeMap.containsKey(timeLong) == false) {
                        eventTimeMap.put(timeLong, new ArrayList<Task>());
                        eventQueue.add(timeLong);
                    } 
                    eventTimeMap.get(timeLong).add(task);
                    time += task.period;
                }
            }
        }

        InterferenceRange range = new InterferenceRange();
        boolean inserted = false;

        while(!eventQueue.isEmpty()) {
            Long timeLong = eventQueue.poll();
            for (Task task : eventTimeMap.get(timeLong)) {
                inserted = range.addTask(timeLong.longValue(), task);
            }

            if(inserted == false) {
                this.interferenceRangeList.add(range);
                range = new InterferenceRange();
            }
        }

        if(inserted == true && range.particiatedTaskNum() > 0) {
            this.interferenceRangeList.add(range);
        }

        for(InterferenceRange rangeInList : this.interferenceRangeList) {
            this.maximumInterferenceTaskNum = Math.max(this.maximumInterferenceTaskNum, rangeInList.particiatedTaskNum());
        }

        int rangeLen = this.interferenceRangeList.size() - 1;
        for(int i = rangeLen ; i >= 0 ; i--) {
            if (this.interferenceRangeList.get(i).particiatedTaskNum() < this.maximumInterferenceTaskNum) {
                this.interferenceRangeList.remove(i);
            }
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

    public SimulationResult simulateCFS(List<Core> cores, int targetTaskID, long simulationTime)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.fine("\n------------------------------");
        logger.fine("*** CFS Simulation Started ***");
        logger.fine("------------------------------");
        SimulationResult finalSimulationResult;
        HashMap<Integer, Long> wcrtMap = initializeWCRTs(cores);
        List<Queue<TaskStat>> queues = initializeQueues(cores, targetTaskID);
        long time = 0;
        this.triedScheduleCount = 0;
        this.scheduleCache = new ScheduleCache(this.method);

        if(this.interferenceRangeList.size() == 0) {            
            makeInterferenceRangeList(cores, simulationTime);
        }


        if(this.method != ScheduleSimulationMethod.RANDOM && this.method != ScheduleSimulationMethod.RANDOM_TARGET_TASK) {
            this.numOfTryToSchedule = 1;
            if (initialOrder == false) {
                CFSSimulationState simulationState = new CFSSimulationState(cores.size());
                simulationState.insertPeriodsAtStartTime(cores);
                //simulationState.insertPeriodsIntoEventQueue(simulationTime, cores);
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
                    queues = initializeQueues(cores, targetTaskID);
                    CFSSimulationState simulationState = new CFSSimulationState(cores.size());
                    simulationState.insertPeriodsAtStartTime(cores);
                    //simulationState.insertPeriodsIntoEventQueue(simulationTime, cores);
                    simulationState.setPreviousEventTime(time);
                    long previousTime = simulationState.getPreviousEventTime();
                    time = simulationState.peekNextEventTime();
                    simulationState.insertPeriodsInTimeRange(cores, previousTime, time);
                    time = simulationState.getNextEventTime();
                    SimulationResult simulResult = performSimulation(cores, queues, wcrtMap, simulationState, time, simulationTime, 0, false);
                    finalSimulationResult = mergeToFinalResult(finalSimulationResult, simulResult);   
                    i++;   
                }
                System.out.println(i);
            }
        } else {
            CFSSimulationState simulationState = new CFSSimulationState(cores.size());
            simulationState.insertPeriodsAtStartTime(cores);
            //simulationState.insertPeriodsIntoEventQueue(simulationTime, cores);
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
                pickData.getQueues().get(pickData.getCoreIndex()).addAll(pickData.getMinRuntimeTasks());
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

    private boolean checkTimeIsInMaximumInterferenceRange(long currentTime) {
        boolean located = false;
        /*for(InterferenceRange range : this.interferenceRangeList) {
            if(range.particiatedTaskNum() == this.maximumInterferenceTaskNum) {
                if(range.getStartTime() <= currentTime && currentTime < range.getEndTime()) {
                    located = true;
                    break;
                }
            }
        }*/

        // TODO: isabled for a while
        return true;
    }

    private SimulationResult performSimulation(List<Core> cores, List<Queue<TaskStat>> queues, HashMap<Integer, Long> wcrtMap,
            CFSSimulationState simulationState, long time, long simulationTime, int coreIndex, boolean resume)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        boolean diverged = false;
        ScheduleCacheData scheduleData = null;
        List<TaskStat> minRuntimeTasks = null;
        SimulationResult mergedResult = new SimulationResult(true, wcrtMap);

        HashMap<Integer, Long> cloneWcrtMap = cloneHashMap(wcrtMap);

        do {
            diverged = false;
            outerLoop:
            while (time < simulationTime && time >= 0) {
                logger.finer("\nTime " + time + ":");
                if (resume == false) {
                    addJobs(cores, queues, simulationState.coreStates, time);

                    List<TaskStat> blockingTasks = getBlockingTasks(queues, simulationState);
                    if (blockingTasks.size() > 1) {
                        String scheduleID = this.scheduleCache.pushScheduleData(simulationState.getSimulationScheduleID(), queues, cloneWcrtMap, simulationState, time, blockingTasks, coreIndex, this.comparator);
                        int taskIndexToSelect = pickNextTaskToSchedule(simulationState, scheduleID);
                        if(taskIndexToSelect == -1) {
                            this.scheduleCache.popScheduleData();
                            diverged = true;
                            break outerLoop;
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
                        if (this.method == ScheduleSimulationMethod.PRIORITY_QUEUE) {
                            if (minRuntimeTasks.size() > 0)
                                task = minRuntimeTasks.get(0);
                        } else { // BRUTE_FORCE or RANDOM or RANDOM_TARGET_TASK
                            if (minRuntimeTasks.size() > 1) {
                                String scheduleID = this.scheduleCache.pushScheduleData(simulationState.getSimulationScheduleID(), queues, cloneWcrtMap, simulationState, time, minRuntimeTasks, coreIndex, this.comparator);
                                int taskIndexToSelect = pickNextTaskToSchedule(simulationState, scheduleID);
                                if(taskIndexToSelect == -1) {
                                    this.scheduleCache.popScheduleData();
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
                String scheduleHashString = simulationState.getSimulationScheduleID() + "_" + simulationState.getSelectedDivergeIndex();
                if (!this.finalScheduleHash.contains(scheduleHashString) || scheduleHashString.length() <= 3) {
                    this.finalScheduleHash.add(simulationState.getSimulationScheduleID() + "_" + simulationState.getSelectedDivergeIndex());
                    this.triedScheduleCount++;

                    if(this.triedScheduleCount % 10000 == 0) {
                        logger.fine("tried: " + this.triedScheduleCount);
                    }

                    SimulationResult simulResult = checkSchedulability(cores, queues, cloneWcrtMap);
                    mergedResult = mergeToFinalResult(mergedResult, simulResult);
                    if(this.method != ScheduleSimulationMethod.PRIORITY_QUEUE) { 
                        this.scheduleCache.saveFinalScheduledIndex(simulationState.getSimulationScheduleID(), simulationState);
                    }
                } else {
                    logger.fine("duplicated schedule: " + scheduleHashString);
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
                minRuntimeTasks = scheduleData.getMinRuntimeTasks();
                coreIndex = scheduleData.getCoreIndex();
                queues.get(coreIndex).addAll(minRuntimeTasks);
                resume = true;
            }
        } while(this.scheduleCache.getScheduleStackSize() > 0 || resume == true);

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
            coreState.isRunning = false;
            if(coreState.remainingRuntime > 0) { 
                simulationState.clearEventTime(time + coreState.remainingRuntime);
                if(this.wakeupGranularity > 0) {
                    long vruntimeWakeup = (this.wakeupGranularity << 10L)  / task.task.weight;
                    if(task.virtualRuntime <= queueInCore.peek().virtualRuntime + vruntimeWakeup)  {
                        long remainedTime = Math.min(coreState.remainingRuntime, this.wakeupGranularity);    
                        coreState.isRunning = true;
                        coreState.remainingRuntime = remainedTime;
                        simulationState.putEventTime(time + remainedTime);
                    }
                }
            } 

            if(coreState.isRunning == false) {
                addIntoQueue(queueInCore, task, time);
            }

            if (simulationState.blockingPolicy == BlockingPolicy.READ)
                simulationState.blockingPolicyReset = true;
        } else {
            logger.fine("Task " + task.task.id + " completed at time " + time + " with RT "
                    + (time - task.readReleaseTime));
            wcrtMap.put(Integer.valueOf(task.getId()),
                    Math.max(wcrtMap.get(Integer.valueOf(task.getId())), time - task.readReleaseTime));
            coreState.putFinishedTaskVirtualRuntime(task.task.id, task.virtualRuntime);
            coreState.isRunning = false;
        }
        
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

    private int pickNextTaskToSchedule(CFSSimulationState simulationState, String scheduleId) {
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

    private void setRuntime(int coreIndex, TaskStat task, Queue<TaskStat> queueInCore, CFSSimulationState simulationState) {
        CoreState coreState = simulationState.coreStates.get(coreIndex);
        long totalWeight = queueInCore.stream().mapToLong(t -> t.task.weight).sum() + task.task.weight;
        coreState.remainingRuntime = (int) Math.max(this.targetLatency * task.task.weight / totalWeight,
                this.minimumGranularity);
        coreState.remainingRuntime = (int) ((coreState.remainingRuntime / this.schedulePeriod) * this.schedulePeriod + this.schedulePeriod); 

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
                if (this.method == ScheduleSimulationMethod.PRIORITY_QUEUE || checkTimeIsInMaximumInterferenceRange(time) == false) {
                    if (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime)
                        minRuntimeTasks.add(queueInCore.poll());
                } else { // BRUTE_FORCE
                    previousTask = null;
                    while (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime) {
                        TaskStat task = queueInCore.poll();
                        if(previousTask != null && taskComparator.compare(task, previousTask) != 0) {
                            queueInCore.add(task);
                            break;
                        }
                        minRuntimeTasks.add(task);
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
                    if (this.method == ScheduleSimulationMethod.PRIORITY_QUEUE || checkTimeIsInMaximumInterferenceRange(time) == false) {
                        if (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime)
                            minRuntimeTasks.add(queueInCore.poll());
                    } else { // BRUTE_FORCE
                        previousTask = null;
                        while (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime) {
                            TaskStat task = queueInCore.poll();
                            if(previousTask != null && taskComparator.compare(task, previousTask) != 0) {
                                queueInCore.add(task);
                                break;
                            }
                            minRuntimeTasks.add(task);
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
                    if (this.method == ScheduleSimulationMethod.PRIORITY_QUEUE || checkTimeIsInMaximumInterferenceRange(time) == false) {
                        if (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime)
                            minRuntimeTasks.add(queueInCore.poll());
                    } else { // BRUTE_FORCE
                        previousTask = null;
                        while (!queueInCore.isEmpty() && queueInCore.peek().virtualRuntime == minRuntime) {
                            TaskStat task = queueInCore.poll();
                            if(previousTask != null && taskComparator.compare(task, previousTask) != 0) {
                                queueInCore.add(task);
                                break;
                            }
                            minRuntimeTasks.add(task);
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
