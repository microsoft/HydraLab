import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AgentManageServiceTest {
@Mock
private AgentUserRepository agentUserRepository;
private AgentManageService agentManageService;
@Before
public void setUp() {
 agentManageService = new AgentManageService(); agentManageService.agentUserRepository = agentUserRepository; 
}

@Test
public void testGetAgent() {
 String agentId = "agent123"; AgentUser agentUser = new AgentUser(); agentUser.setId(agentId); Optional<AgentUser> optionalAgentUser = Optional.of(agentUser); when(agentUserRepository.findById(agentId)).thenReturn(optionalAgentUser); AgentUser result = agentManageService.getAgent(agentId); assertEquals(agentUser, result); 
}

}
