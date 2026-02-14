import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AgentManageServiceTest {

    @Mock
    private AgentUserRepository agentUserRepository;

    @InjectMocks
    private AgentManageService agentManageService;

    private String teamId;
    private String teamName;
    private List<AgentUser> agents;

    @Before
    public void setUp() {
        teamId = "teamId";
        teamName = "teamName";
        agents = new ArrayList<>();
        agents.add(new AgentUser());
        agents.add(new AgentUser());
    }

    @Test
    public void testUpdateAgentTeam() {
        when(agentUserRepository.findAllByTeamId(teamId)).thenReturn(agents);

        agentManageService.updateAgentTeam(teamId, teamName);

        verify(agentUserRepository).saveAll(agents);

        for (AgentUser agent : agents) {
            assertEquals(teamName, agent.getTeamName());
        }
    }
}