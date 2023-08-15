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
        teamListMap = new ConcurrentHashMap<>();
        sysTeamService = new SysTeamService();
        sysTeamService.sysTeamRepository = sysTeamRepository;
        sysTeamService.userTeamRelationRepository = userTeamRelationRepository;
        sysTeamService.agentManageService = agentManageService;
        sysTeamService.deviceGroupService = deviceGroupService;
        sysTeamService.deviceAgentManagementService = deviceAgentManagementService;
        sysTeamService.testFileSetService = testFileSetService;
        sysTeamService.testTaskService = testTaskService;
        sysTeamService.attachmentService = attachmentService;
        sysTeamService.teamListMap = teamListMap;
    }

    @Test
    public void testDeleteTeam() {
        // Arrange
        SysTeam team = new SysTeam();
        team.setTeamId("1");
        team.setTeamName("Team 1");
        team.setCreateTime(new Date());
        team.setUpdateTime(new Date());
        teamListMap.put(team.getTeamId(), team);

        // Act
        sysTeamService.deleteTeam(team);

        // Assert
        Mockito.verify(sysTeamRepository).deleteById(team.getTeamId());
        Mockito.verify(sysTeamService.teamListMap).remove(team.getTeamId());
    }
}