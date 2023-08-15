import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AgentManageServiceTest {

    @Mock
    private AgentUserRepository agentUserRepository;

    @Mock
    private UserTeamManagementService userTeamManagementService;

    @Mock
    private SysUserService sysUserService;

    @InjectMocks
    private AgentManageService agentManageService;

    @Test
    public void testGenerateAgentConfigFile() {
        // Arrange
        String agentId = "agentId";
        String host = "localhost";

        AgentUser agentUser = new AgentUser();
        agentUser.setId(agentId);
        agentUser.setName("Agent Name");
        agentUser.setSecret("Agent Secret");

        when(agentUserRepository.findById(agentId)).thenReturn(Optional.of(agentUser));

        // Act
        File agentConfigFile = agentManageService.generateAgentConfigFile(agentId, host);

        // Assert
        assertNotNull(agentConfigFile);
        assertTrue(agentConfigFile.exists());
        assertEquals("application.yml", agentConfigFile.getName());
    }
}