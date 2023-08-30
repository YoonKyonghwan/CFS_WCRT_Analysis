package org.cap.simulation;

import org.cap.model.Core;
import org.cap.model.Task;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

public class PFSSimulatorTest {
    private PFSSimulator simulator;

    @Before
    public void setUp() {
        simulator = new PFSSimulator();
    }

    @Test
    public void testOneTask() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 1, 1, 1, 0, 10, 0) // a single task
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore) // a single task
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulatePFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
            3.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testTwoTasks() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 1, 2, 1, 0, 10, 0), // two tasks
            new Task(2, 0, 1, 2, 1, 0, 10, 1)
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore) // a single task
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulatePFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                8.0,
                8.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testReadAndWriteReleasedTogether() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 1, 2, 1, 0, 10, 0), // write is released at t=3
            new Task(2, 3, 1, 2, 1, 0, 10, 1)  // read is released at t=3
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulatePFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                6.0,
                5.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testReadReleasedWhenWriteIsExecuting() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 1, 1, 2, 0, 10, 0), // write is executing at t=3
            new Task(2, 3, 1, 1, 2, 0, 10, 1)  // read is released at t=3
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulatePFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                4.0,
                5.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testTasksWithDifferentPeriods() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 1.0, 1.0, 1.0, 0, 15, 0),  // period 15
            new Task(2, 0, 1.0, 1.0, 1.0, 0, 5, 1)   // period 5
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulatePFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                6.0,
                6.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testTasksWithDifferentNice() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 1.0, 2.0, 1.0, 0, 15, 0),  // nice value 0
            new Task(2, 0, 1.0, 2.0, 1.0, 10, 15, 1)  // nice value 10
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulatePFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                8.0,
                11.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testRounding() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 0, 1, 0, 0, 10, 0), // three tasks
            new Task(2, 0, 0, 1, 0, 0, 10, 1),
            new Task(3, 0, 0, 1, 0, 0, 10, 2)
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulatePFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                3.0,
                3.0,
                3.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testFIFOWithReadAndWrite() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 5, 1, 0, 0, 10, 0), // three tasks
            new Task(2, 0, 0, 1, 1, 0, 10, 1),
            new Task(3, 4, 0, 1, 1, 0, 10, 2)
        );
        List<Core> cores = List.of(
            new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulatePFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                9.0,
                9.0,
                6.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    @Test
    public void testFIFOWithWrite() {
        // Define test tasks
        List<Task> tasksInFirstCore = List.of(
            new Task(1, 0, 0, 1, 5, 0, 10, 0), // three tasks
            new Task(2, 1, 0, 1, 1, 0, 10, 1),
            new Task(3, 4, 0, 1, 1, 0, 10, 2)
        );
        List<Core> cores = List.of(
                new Core(1, tasksInFirstCore)
        );

        // Execute the method
        List<List<Double>> WCRTs = simulator.simulatePFS(cores).WCRTs;

        // Make assertions about the expected result
        List<List<Double>> expectedResult = List.of(
            List.of(
                8.0,
                8.0,
                6.0
            )
        );
        assertEquals(expectedResult, WCRTs);
    }

    // TODO make more test cases
    // TODO check if path diverges at the start of simulation

}

// 해야 할 거:
// BlockingPolicy.None 수정 버그 픽스
// minimum vruntime 설정
    // 이건 어떻게 하지, read, write의 경우에 minimumVirtualRuntime 업데이트
// 테스트 케이스 추가


//             // 기존 task는 교차하고, task가 release되면 새 task를 우선시하는게 옳은 정책
//
//            // TODO if read, write tasks exist in different cores, diverge path
//            // 분기하면서 어느 read, write task가 돌아갈지 지정해준다
//            // 각각에 대한 케이스를 모두 simulate한다

// 분기 종류
// 케이스 1: 한 코어에 min runtime이 동일한 task가 여러개 있을 때
// 케이스 2: 여러 코어에 read, write task가 있을 때

// TODO minimum virtual runtime을 설정해야 한다

// 1, 2, 1, 2, 이렇게 교차할 수 있게

// 0이면 Diverge한다, 하지만 이미 돌아가던 기존 Task라면..
// 그 전 targeted latency동안 순서를 기록해보는 것도 굳굳일 듯
// targeted latency마다 순서 기록하는 필드를 가지고, 거기다 저장함.
// task가 빠지거나 추가되지 않았으면, 그거대로 고고
// task가 빠지거나 추가되지 않은거 확인하려면 Task id 리스트를 corestate에 저장해두면 될 뜻 Targeted latency 시작할 때