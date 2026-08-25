import com.microsoft.hydralab.center.repository.UserTeamRelationRepository;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.center.UserTeamRelation;
import com.microsoft.hydralab.common.util.Const;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(MockitoJUnitRunner.class)
public class UserTeamManagementServiceTest {
@Mock
private UserTeamRelationRepository userTeamRelationRepository;
@InjectMocks
private UserTeamManagementService userTeamManagementService;
@Before
public void setUp() {
 UserTeamRelation userTeamRelation = new UserTeamRelation("teamId", "mailAddress", true); Mockito.when(userTeamRelationRepository.findByMailAddressAndTeamId(anyString(), anyString())).thenReturn(Optional.of(userTeamRelation)); 
}

@Test
public void testQueryRelation() {
 UserTeamRelation result = userTeamManagementService.queryRelation("mailAddress", "teamId"); assertEquals("teamId", result.getTeamId()); assertEquals("mailAddress", result.getMailAddress()); assertEquals(true, result.isTeamAdmin()); 
}

}
