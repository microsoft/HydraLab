import com.microsoft.hydralab.common.entity.center.RolePermissionRelation;
import com.microsoft.hydralab.common.entity.center.RolePermissionRelationId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.Optional;
import static org.mockito.Mockito.verify;

public class RolePermissionRelationRepositoryTest {
private RolePermissionRelationRepository rolePermissionRelationRepository;
public void setup() {
", " MockitoAnnotations.initMocks(this);", " 
}

public void testDeleteAllByRoleId() {
", " String roleId = \"testRoleId\";", " rolePermissionRelationRepository.deleteAllByRoleId(roleId);", " verify(rolePermissionRelationRepository).deleteAllByRoleId(roleId);", " 
}

}
