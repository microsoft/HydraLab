import com.microsoft.hydralab.center.repository.AgentUserRepository;
import com.microsoft.hydralab.center.util.CenterConstant;
import com.microsoft.hydralab.center.util.SecretGenerator;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.AgentUser;
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.util.CriteriaTypeUtil;
import com.microsoft.hydralab.center.service.UserTeamManagementService;
import com.microsoft.hydralab.center.service.SysUserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
private SysUser sysUser;
private AgentUser agentUser;
@Before
public void setup() {
 sysUser = new SysUser(); sysUser.setMailAddress("test@mail.com"); agentUser = new AgentUser(); agentUser.setId("agentId"); agentUser.setMailAddress("test@mail.com"); 
}

@Test
public void testCheckAgentAuthorization_WithNullRequestor_ReturnsFalse() {
 assertFalse(agentManageService.checkAgentAuthorization(null, "agentId")); 
}

@Test
public void testCheckAgentAuthorization_WithNonExistingAgent_ReturnsFalse() {
 Mockito.when(agentUserRepository.findById("agentId")).thenReturn(null); assertFalse(agentManageService.checkAgentAuthorization(sysUser, "agentId")); 
}

@Test
public void testCheckAgentAuthorization_WithMatchingMailAddress_ReturnsTrue() {
 Mockito.when(agentUserRepository.findById("agentId")).thenReturn(agentUser); assertTrue(agentManageService.checkAgentAuthorization(sysUser, "agentId")); 
}

@Test
public void testCheckAgentAuthorization_WithAdminUser_ReturnsTrue() {
 Mockito.when(agentUserRepository.findById("agentId")).thenReturn(agentUser); Mockito.when(sysUserService.checkUserAdmin(sysUser)).thenReturn(true); assertTrue(agentManageService.checkAgentAuthorization(sysUser, "agentId")); 
}

@Test
public void testCheckAgentAuthorization_WithTeamAdmin_ReturnsTrue() {
 Mockito.when(agentUserRepository.findById("agentId")).thenReturn(agentUser); Mockito.when(userTeamManagementService.checkRequestorTeamAdmin(sysUser, "teamId")).thenReturn(true); assertTrue(agentManageService.checkAgentAuthorization(sysUser, "agentId")); 
}

}
