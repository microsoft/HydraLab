import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class AgentManageServiceTest {
    @Mock
    private AgentUserRepository agentUserRepository;
    
    private AgentManageService agentManageService;
    
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        agentManageService = new AgentManageService();
        agentManageService.agentUserRepository = agentUserRepository;
    }
    
    @Test
    public void testDeleteAgent() {
        AgentUser agentUser = new AgentUser();
        agentManageService.deleteAgent(agentUser);
        verify(agentUserRepository, times(1)).delete(agentUser);
    }
}