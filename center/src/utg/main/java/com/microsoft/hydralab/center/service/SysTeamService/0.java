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
public void testInitList() {
 List<SysTeam> teamList = new ArrayList<>(); SysTeam team1 = new SysTeam(); team1.setTeamId("1"); team1.setTeamName("Team 1"); team1.setCreateTime(new Date()); team1.setUpdateTime(new Date()); teamList.add(team1); SysTeam team2 = new SysTeam(); team2.setTeamId("2"); team2.setTeamName("Team 2"); team2.setCreateTime(new Date()); team2.setUpdateTime(new Date()); teamList.add(team2); Mockito.when(sysTeamRepository.findAll()).thenReturn(teamList); sysTeamService.initList(); Map<String, SysTeam> teamListMap = sysTeamService.teamListMap; assert teamListMap.containsKey("1"); assert teamListMap.containsKey("2"); assert teamListMap.get("1").equals(team1); assert teamListMap.get("2").equals(team2); 
}

}
