package org.cap

import spock.lang.Specification

import org.cap.utility.JsonTaskCreator;
import org.cap.model.Core;
import org.cap.model.Task;
import org.cap.model.TestConfiguration;

import java.util.List;
import java.util.ArrayList;
import java.lang.Math;

class JsonTaskCreatorTest extends Specification {

    def "check utilization of generateTasks"(){
        given:
            JsonTaskCreator jsonTaskCreator = new JsonTaskCreator();
            List<Core> cores = new ArrayList<>();
            Core core = new Core(1, new ArrayList<>()); //numCores = 1
            cores.add(core);
        when:
            def testConf = jsonTaskCreator.generateTasks(numTasks, total_utilzation, cores)
            def sum_task_utilization = 0;
            for (Core testConf_core in testConf.mappingInfo) {
                for (Task task in testConf_core.tasks) {
                    def task_utilization = task.bodyTime / task.period;
                    sum_task_utilization += task_utilization;
                }
            }
            def theshold = 0.03;
            def gap = Math.abs(total_utilzation - sum_task_utilization);
        then:
            //check gap between total_utilzation and sum_task_utilization is less than theshold
            gap < theshold
        where:
            total_utilzation | numTasks
            0.5 | 10
            0.7 | 20
            0.9 | 30
    }
}