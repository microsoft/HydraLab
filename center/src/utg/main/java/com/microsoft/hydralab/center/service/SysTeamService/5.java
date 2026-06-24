import com.microsoft.hydralab.center.repository.SysTeamRepository;
import com.microsoft.hydralab.center.repository.UserTeamRelationRepository;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.util.AttachmentService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.Assert.assertEquals;
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
@InjectMocks
private SysTeamService sysTeamService;
private Map<String, SysTeam> teamListMap;
@Before
public void setUp() {
 teamListMap = new ConcurrentHashMap<>(); sysTeamService = new SysTeamService(); sysTeamService.sysTeamRepository = sysTeamRepository; sysTeamService.userTeamRelationRepository = userTeamRelationRepository; sysTeamService.agentManageService = agentManageService; sysTeamService.deviceGroupService = deviceGroupService; sysTeamService.deviceAgentManagementService = deviceAgentManagementService; sysTeamService.testFileSetService = testFileSetService; sysTeamService.testTaskService = testTaskService; sysTeamService.attachmentService = attachmentService; sysTeamService.teamListMap = teamListMap; 
}

@Test
public void testQueryTeamByName() {
 String teamName = "Team1"; SysTeam expectedTeam = new SysTeam(); expectedTeam.setTeamName(teamName); expectedTeam.setTeamId("1"); expectedTeam.setCreateTime(new Date()); expectedTeam.setUpdateTime(new Date()); List<SysTeam> teamList = new ArrayList<>(); teamList.add(expectedTeam); when(sysTeamRepository.findByTeamName(teamName)).thenReturn(Optional.of(expectedTeam)); SysTeam actualTeam = sysTeamService.queryTeamByName(teamName); assertEquals(expectedTeam, actualTeam); 
}

}
