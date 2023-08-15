import com.microsoft.hydralab.center.repository.SysTeamRepository;
import com.microsoft.hydralab.center.repository.UserTeamRelationRepository;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.util.AttachmentService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RunWith(MockitoJUnitRunner.class)
public class SysTeamServiceTest {
@Mock
private SysTeamRepository sysTeamRepository;
@Mock
private UserTeamRelationRepository userTeamRelationRepository;
@Mock
private AgentManageService agentManageService;
@Mock
private DeviceGroupService deviceGroupService;
@Mock
private DeviceAgentManagementService deviceAgentManagementService;
@Mock
private TestFileSetService testFileSetService;
@Mock
private TestTaskService testTaskService;
@Mock
private AttachmentService attachmentService;
private SysTeamService sysTeamService;
@Before
public void setup() {
 sysTeamService = new SysTeamService(); sysTeamService.sysTeamRepository = sysTeamRepository; sysTeamService.userTeamRelationRepository = userTeamRelationRepository; sysTeamService.agentManageService = agentManageService; sysTeamService.deviceGroupService = deviceGroupService; sysTeamService.deviceAgentManagementService = deviceAgentManagementService; sysTeamService.testFileSetService = testFileSetService; sysTeamService.testTaskService = testTaskService; sysTeamService.attachmentService = attachmentService; 
}

@Test
public void testUpdateTeamRelatedEntity() {
 String teamId = "teamId"; String teamName = "teamName"; SysTeam sysTeam = new SysTeam(); sysTeam.setTeamId(teamId); sysTeam.setTeamName(teamName); sysTeam.setCreateTime(new Date()); sysTeam.setUpdateTime(new Date()); Mockito.when(sysTeamRepository.findById(teamId)).thenReturn(Optional.of(sysTeam)); sysTeamService.updateTeamRelatedEntity(teamId, teamName); Mockito.verify(agentManageService).updateAgentTeam(teamId, teamName); Mockito.verify(deviceGroupService).updateGroupTeam(teamId, teamName); Mockito.verify(deviceAgentManagementService).updateAgentDeviceGroupTeam(teamId, teamName); Mockito.verify(testFileSetService).updateFileSetTeam(teamId, teamName); Mockito.verify(testTaskService).updateTaskTeam(teamId, teamName); Mockito.verify(attachmentService).updateTestJsonTeam(teamId, teamName); 
}

}
