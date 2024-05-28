import com.microsoft.hydralab.center.repository.DeviceGroupRelationRepository;
import com.microsoft.hydralab.center.repository.DeviceGroupRepository;
import com.microsoft.hydralab.common.entity.center.DeviceGroup;
import com.microsoft.hydralab.common.entity.center.DeviceGroupRelation;
import com.microsoft.hydralab.common.entity.center.DeviceGroupRelationId;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.CriteriaTypeUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(MockitoJUnitRunner.class)
public class DeviceGroupServiceTest {
@Mock
private DeviceGroupRepository deviceGroupRepository;
@Mock
private DeviceGroupRelationRepository deviceGroupRelationRepository;
@Mock
private SysUserService sysUserService;
@Mock
private UserTeamManagementService userTeamManagementService;
private DeviceGroupService deviceGroupService;
@Before
public void setup() {
 deviceGroupService = new DeviceGroupService(); deviceGroupService.deviceGroupRepository = deviceGroupRepository; deviceGroupService.deviceGroupRelationRepository = deviceGroupRelationRepository; deviceGroupService.sysUserService = sysUserService; deviceGroupService.userTeamManagementService = userTeamManagementService; 
}

@Test
public void testCheckGroupAuthorization() {
 SysUser requestor = new SysUser(); String groupName = "testGroup"; boolean teamAdminRequired = true; DeviceGroup deviceGroup = new DeviceGroup(); deviceGroup.setTeamId("testTeamId"); Mockito.when(deviceGroupRepository.findById(groupName)).thenReturn(deviceGroup); Mockito.when(sysUserService.checkUserAdmin(requestor)).thenReturn(false); Mockito.when(userTeamManagementService.checkRequestorTeamAdmin(requestor, deviceGroup.getTeamId())).thenReturn(true); boolean result = deviceGroupService.checkGroupAuthorization(requestor, groupName, teamAdminRequired); Mockito.verify(deviceGroupRepository).findById(groupName); Mockito.verify(sysUserService).checkUserAdmin(requestor); Mockito.verify(userTeamManagementService).checkRequestorTeamAdmin(requestor, deviceGroup.getTeamId()); Mockito.verifyNoMoreInteractions(deviceGroupRepository, sysUserService, userTeamManagementService); Assert.assertTrue(result); 
}

}
