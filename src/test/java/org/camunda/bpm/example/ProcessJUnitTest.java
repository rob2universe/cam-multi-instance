package org.camunda.bpm.example;

import org.camunda.bpm.engine.repository.DeploymentWithDefinitions;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.hamcrest.MatcherAssert;
import org.hamcrest.beans.HasPropertyWithValue;
import org.hamcrest.core.Every;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;

import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class ProcessJUnitTest {

    @Rule
    public ProcessEngineRule engine = new ProcessEngineRule();

    @Before
    public void setUp() {
        init(engine.getProcessEngine());
        BpmnModelInstance modelInst = Bpmn.createProcess("MultiInstanceProcess")
                .executable()
                .startEvent()
                .userTask().id("ProcessCollectionItemTask").multiInstance()
                .parallel().camundaCollection("${orderItems}").camundaElementVariable("orderItem")
                .multiInstanceDone()
                .endEvent().name("Processing done").done();
        repositoryService().createDeployment().addModelInstance("process.bpmn",modelInst).deploy();
    }

    @Test
    public void testInstanceWithProcessVariableValue() {
        // When starting a process with a multi instance task based on collection orderItems
        ProcessInstance pi1 = runtimeService().startProcessInstanceByKey("MultiInstanceProcess",
                withVariables("orderItems", Arrays.asList("Tic", "Tac", "Toe")));
        assertThat(pi1).isWaitingAt("ProcessCollectionItemTask");
        assertEquals(3, taskQuery().processInstanceId(pi1.getId()).count());
        MatcherAssert.assertThat(taskQuery().processInstanceId(pi1.getId()).list(), Every.everyItem(HasPropertyWithValue.hasProperty("taskDefinitionKey", Is.is("ProcessCollectionItemTask"))));

        // querying by process variable should not return 3 tasks
        assertEquals(0, taskQuery().processInstanceId(pi1.getId()).processVariableValueEquals("orderItem", "Tic").count());
        // fails, returns 3 instances
    }

    @Test
    public void testInstanceWithTaskVariableValue() {
        // When starting a process with a multi instance task based on collection orderItems
        ProcessInstance pi2 = runtimeService().startProcessInstanceByKey("MultiInstanceProcess",
                withVariables("orderItems", Arrays.asList("Tic", "Tac", "Toe")));
        assertThat(pi2).isWaitingAt("ProcessCollectionItemTask");
        assertEquals(3, taskQuery().processInstanceId(pi2.getId()).count());
        MatcherAssert.assertThat(taskQuery().processInstanceId(pi2.getId()).list(), Every.everyItem(HasPropertyWithValue.hasProperty("taskDefinitionKey", Is.is("ProcessCollectionItemTask"))));

       // the instances of the task should have a taskVariable orderItem with the 3 different values
        Task tacTask = taskQuery().processInstanceId(pi2.getId()).taskVariableValueEquals("orderItem", "Tac").singleResult();
        assertNotNull(tacTask);
        // fails, returns no instance
    }

    @Test
    public void testAddTask() {
        ProcessInstance pi3 = runtimeService().startProcessInstanceByKey("MultiInstanceProcess",
                withVariables("orderItems", Arrays.asList("Tic", "Tac", "Toe")));
        assertThat(pi3).isWaitingAt("ProcessCollectionItemTask");
        assertEquals(3, taskQuery().processInstanceId(pi3.getId()).count());

        taskService().saveTask(taskService().newTask());
        assertEquals(4, taskQuery().count());

        taskQuery().list().forEach(task -> System.out.println("TaskDefinitionKey: " + task.getTaskDefinitionKey()));
    }
}