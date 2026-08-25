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
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

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
private Map<String, SysTeam> teamListMap;
@Before
public void setUp() {
 teamListMap = new ConcurrentHashMap<>(); sysTeamService = new SysTeamService(); sysTeamService.sysTeamRepository = sysTeamRepository; sysTeamService.userTeamRelationRepository = userTeamRelationRepository; sysTeamService.agentManageService = agentManageService; sysTeamService.deviceGroupService = deviceGroupService; sysTeamService.deviceAgentManagementService = deviceAgentManagementService; sysTeamService.testFileSetService = testFileSetService; sysTeamService.testTaskService = testTaskService; sysTeamService.attachmentService = attachmentService; sysTeamService.teamListMap = teamListMap; 
}

@Test
public void testUpdateTeam() {
 SysTeam sysTeam = new SysTeam(); sysTeam.setTeamId("1"); sysTeam.setTeamName("Team 1"); sysTeam.setCreateTime(new Date()); sysTeam.setUpdateTime(new Date()); when(sysTeamRepository.save(Mockito.any(SysTeam.class))).thenReturn(sysTeam); SysTeam result = sysTeamService.updateTeam(sysTeam); assertNotNull(result); assertEquals(sysTeam.getTeamId(), result.getTeamId()); assertEquals(sysTeam.getTeamName(), result.getTeamName()); assertEquals(sysTeam.getCreateTime(), result.getCreateTime()); assertEquals(sysTeam.getUpdateTime(), result.getUpdateTime()); 
}

}
