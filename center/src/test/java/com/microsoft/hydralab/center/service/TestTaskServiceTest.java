package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.common.entity.center.TestTaskQueuedInfo;
import com.microsoft.hydralab.common.entity.center.TestTaskSpec;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.center.service.TestTaskService;
import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.center.service.AgentManageService;
import com.microsoft.hydralab.center.service.DeviceGroupService;
import com.microsoft.hydralab.center.service.TestDataService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestTaskServiceTest {

    @Mock
    private DeviceAgentManagementService deviceAgentManagementService;
    @Mock
    private AgentManageService agentManageService;
    @Mock
    private DeviceGroupService deviceGroupService;
    @Mock
    private TestDataService testDataService;
    private TestTaskService testTaskService;
    @Mock
    private TestTaskSpec testTaskSpec;
    private Queue<TestTaskSpec> taskQueue;
    private AtomicBoolean isRunning;
    @Mock
    private TestTaskQueuedInfo testTaskQueuedInfo;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAddTask() {
        TestTaskSpec task = new TestTaskSpec();
        testTaskService.addTask(task);
        verify(testDataService, times(1)).saveTestTaskData(any(TestTask.class));
    }

    @Test
    public void testIsQueueEmpty() {
        Queue<TestTaskSpec> taskQueue = Mockito.mock(Queue.class);
        Mockito.when(taskQueue.isEmpty()).thenReturn(true);
        testTaskService.taskQueue = taskQueue;
        Boolean result = testTaskService.isQueueEmpty();
        Assert.assertTrue(result);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        testTaskService = new TestTaskService();
        testTaskService.testDataService = testDataService;
    }

    @Test
    public void testIsDeviceFree() {
        String deviceIdentifier = "device1";
        when(deviceAgentManagementService.queryGroupByDevice(deviceIdentifier)).thenReturn(new HashSet<>());
        when(deviceAgentManagementService.queryDeviceByGroup(deviceIdentifier)).thenReturn(new HashSet<>());
        Boolean result = testTaskService.isDeviceFree(deviceIdentifier);
        assertTrue(result);
        verify(deviceAgentManagementService, times(1)).queryGroupByDevice(deviceIdentifier);
        verify(deviceAgentManagementService, times(1)).queryDeviceByGroup(deviceIdentifier);
    }

    @Test
    public void testRunTask() {
        TestTaskSpec testTaskSpec = new TestTaskSpec();
        testTaskSpec.setTestTaskId("testTaskId");
        testTaskSpec.setDeviceIdentifier("deviceIdentifier");
        taskQueue.offer(testTaskSpec);
        Mockito.when(testTaskService.getTestQueueCopy()).thenReturn(taskQueue);
        Mockito.when(deviceAgentManagementService.runTestTaskBySpec(testTaskSpec)).thenReturn(new JSONObject());
        testTaskService.runTask();
        Mockito.verify(deviceAgentManagementService, Mockito.times(1)).runTestTaskBySpec(testTaskSpec);
        Mockito.verify(testDataService, Mockito.times(1)).saveTestTaskData(Mockito.any(TestTask.class));
        Mockito.verify(testTaskService, Mockito.times(1)).getTestQueueCopy();
        Mockito.verify(testTaskService, Mockito.times(1)).isRunning.set(false);
    }

    @Test
    public void testCancelTask() {
        String testTaskId = "12345";
        testTaskService.cancelTask(testTaskId);
        verify(testTaskService, times(1)).cancelTask(testTaskId);
    }

    @Test
    public void testGetTestQueuedInfo() {
        String testTaskId = "12345";
        when(testTaskService.getTestQueuedInfo(testTaskId)).thenReturn(testTaskQueuedInfo);
        TestTaskQueuedInfo result = testTaskService.getTestQueuedInfo(testTaskId);
        assertEquals(testTaskQueuedInfo, result);
    }

    @Test
    public void testCheckTestTaskTeamConsistency() {
        String teamId = "teamId";
        String teamName = "teamName";
        testTaskService.checkTestTaskTeamConsistency(testTaskSpec);
    }

    @Test
    public void testUpdateTaskTeam() {
        String teamId = "123";
        String teamName = "Test Team";
        List<TestTask> testTasks = mock(List.class);
        when(testDataService.getTasksByTeamId(teamId)).thenReturn(testTasks);
        testTaskService.updateTaskTeam(teamId, teamName);
        verify(testTasks, times(1)).forEach(any());
        verify(testDataService, times(1)).saveAllTestTasks(testTasks);
    }

}