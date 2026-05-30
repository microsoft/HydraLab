import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.center.controller.AgentManageController;
import com.microsoft.hydralab.center.service.AttachmentService;
import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.center.service.SysTeamService;
import com.microsoft.hydralab.center.service.SysUserService;
import com.microsoft.hydralab.center.service.UserTeamManagementService;
import com.microsoft.hydralab.common.entity.agent.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

@RunWith(MockitoJUnitRunner.class)
public class AgentManageControllerTest {

    @Mock
    private AttachmentService attachmentService;

    @Mock
    private DeviceAgentManagementService deviceAgentManagementService;

    @Mock
    private SysTeamService sysTeamService;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private UserTeamManagementService userTeamManagementService;

    @InjectMocks
    private AgentManageController agentManageController;

    @Before
    public void setup() {
        Mockito.when(attachmentService.getLatestAgentPackage()).thenReturn("agentPackage");
    }

    @Test
    public void testGetCenterInfo() {
        // Arrange
        JSONObject expectedData = new JSONObject();
        expectedData.put("versionName", "versionName");
        expectedData.put("versionCode", "versionCode");
        expectedData.put("agentPkg", "agentPackage");

        // Act
        Result result = agentManageController.getCenterInfo();

        // Assert
        assert result != null;
        assert result.getStatus() == HttpStatus.OK.value();
        assert result.getData().equals(expectedData);
    }
}