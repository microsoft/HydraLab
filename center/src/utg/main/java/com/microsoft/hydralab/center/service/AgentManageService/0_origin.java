import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import com.microsoft.hydralab.center.repository.AgentUserRepository;
import com.microsoft.hydralab.center.util.SecretGenerator;
import com.microsoft.hydralab.common.entity.common.AgentUser;

@RunWith(MockitoJUnitRunner.class)
public class AgentManageServiceTest {
    @Mock
    private AgentUserRepository agentUserRepository;
    
    @InjectMocks
    private AgentManageService agentManageService;
    
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void testCreateAgent() {
        // Arrange
        String teamId = "teamId";
        String teamName = "teamName";
        String mailAddress = "mailAddress";
        String os = "os";
        String name = "name";
        AgentUser agentUserInfo = new AgentUser();
        agentUserInfo.setMailAddress(mailAddress);
        agentUserInfo.setOs(os);
        agentUserInfo.setName(name);
        agentUserInfo.setTeamId(teamId);
        agentUserInfo.setTeamName(teamName);
        SecretGenerator secretGenerator = new SecretGenerator();
        String agentSecret = secretGenerator.generateSecret();
        agentUserInfo.setSecret(agentSecret);
        when(agentUserRepository.saveAndFlush(any(AgentUser.class))).thenReturn(agentUserInfo);
        
        // Act
        AgentUser result = agentManageService.createAgent(teamId, teamName, mailAddress, os, name);
        
        // Assert
        assertNotNull(result);
        assertEquals(mailAddress, result.getMailAddress());
        assertEquals(os, result.getOs());
        assertEquals(name, result.getName());
        assertEquals(teamId, result.getTeamId());
        assertEquals(teamName, result.getTeamName());
        assertEquals(agentSecret, result.getSecret());
        verify(agentUserRepository, times(1)).saveAndFlush(any(AgentUser.class));
    }
}