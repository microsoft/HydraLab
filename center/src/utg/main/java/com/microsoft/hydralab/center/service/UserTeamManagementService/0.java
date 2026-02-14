import com.microsoft.hydralab.center.repository.UserTeamRelationRepository;
import com.microsoft.hydralab.common.entity.center.SysRole;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.center.UserTeamRelation;
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.util.Const;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.CollectionUtils;
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class UserTeamManagementServiceTest {
@Mock
private UserTeamRelationRepository userTeamRelationRepository;
@Mock
private SysTeamService sysTeamService;
@Mock
private SysUserService sysUserService;
@Mock
private SysRoleService sysRoleService;
@InjectMocks
private UserTeamManagementService userTeamManagementService;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); 
}

@Test
public void testInitList() {
 List<UserTeamRelation> relationList = new ArrayList<>(); UserTeamRelation relation = new UserTeamRelation("teamId", "mailAddress", true); relationList.add(relation); Mockito.when(userTeamRelationRepository.findAll()).thenReturn(relationList); SysTeam team = new SysTeam(); Mockito.when(sysTeamService.queryTeamById(Mockito.anyString())).thenReturn(team); SysUser user = new SysUser(); Mockito.when(sysUserService.queryUserByMailAddress(Mockito.anyString())).thenReturn(user); userTeamManagementService.initList(); 
}

}
