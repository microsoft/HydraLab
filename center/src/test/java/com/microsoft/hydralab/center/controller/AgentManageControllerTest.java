package com.microsoft.hydralab.center.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.center.controller.AgentManageController;
import com.microsoft.hydralab.center.service.AttachmentService;
import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.center.service.SysTeamService;
import com.microsoft.hydralab.center.service.SysUserService;
import com.microsoft.hydralab.center.service.UserTeamManagementService;
import com.microsoft.hydralab.common.entity.agent.Result;

@RunWith(MockitoJUnitRunner.class)
public class AgentManageControllerTest {

  @Mock
  private DeviceAgentManagementService deviceAgentManagementService;
  @Mock
  private AgentManageService agentManageService;
  @InjectMocks
  private AgentManageController agentManageController;
  @Mock
  private AttachmentService attachmentService;
  @Mock
  private SysTeamService sysTeamService;
  @Mock
  private SysUserService sysUserService;
  @Mock
  private UserTeamManagementService userTeamManagementService;

  @Before
  public void setup() {
    Mockito.when(attachmentService.getLatestAgentPackage()).thenReturn("agentPackage");
  }

  @Test
  public void testQueryUpdateInfo() {
    List<AgentUpdateTask> updateTasks = new ArrayList<>();
    when(deviceAgentManagementService.getUpdateTasks()).thenReturn(updateTasks);
    Result result = agentManageController.queryUpdateInfo();
    assertEquals(HttpStatus.OK.value(), result.getStatusCode());
    assertEquals(updateTasks, result.getData());
  }

  @Test
  public void testGetCenterInfo() {
    JSONObject expectedData = new JSONObject();
    expectedData.put("versionName", "versionName");
    expectedData.put("versionCode", "versionCode");
    expectedData.put("agentPkg", "agentPackage");
    Result result = agentManageController.getCenterInfo();
    assert result != null;
    assert result.getStatus() == HttpStatus.OK.value();
    assert result.getData().equals(expectedData);
  }

}