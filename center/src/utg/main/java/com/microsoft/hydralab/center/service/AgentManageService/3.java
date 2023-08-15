import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import java.util.Optional;
import com.microsoft.hydralab.center.repository.AgentUserRepository;
import com.microsoft.hydralab.common.entity.common.AgentUser;

public class AgentManageServiceTest {
@Mock
private AgentUserRepository agentUserRepository;
private AgentManageService agentManageService;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); agentManageService = new AgentManageService(); agentManageService.agentUserRepository = agentUserRepository; 
}

@Test
public void testIsAgentNameRegistered() {
 String agentName = "agent1"; AgentUser agentUser = new AgentUser(); agentUser.setName(agentName); Mockito.when(agentUserRepository.findByName(agentName)).thenReturn(Optional.of(agentUser)); boolean result = agentManageService.isAgentNameRegistered(agentName); Assert.assertTrue(result); 
}

}
