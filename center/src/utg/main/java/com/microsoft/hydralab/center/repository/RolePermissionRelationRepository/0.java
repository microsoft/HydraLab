import com.microsoft.hydralab.common.entity.center.RolePermissionRelation;
import com.microsoft.hydralab.common.entity.center.RolePermissionRelationId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import static org.mockito.Mockito.verify;

public class RolePermissionRelationRepositoryTest {
private RolePermissionRelationRepository repository;
private RolePermissionRelation rolePermissionRelation;
private RolePermissionRelationId rolePermissionRelationId;
public void setUp() {
", " rolePermissionRelation = new RolePermissionRelation();", " rolePermissionRelationId = new RolePermissionRelationId();", " 
}

public void testDeleteAllByPermissionId() {
", " String permissionId = \"testPermissionId\";", " repository.deleteAllByPermissionId(permissionId);", " verify(repository).deleteAllByPermissionId(permissionId);", " 
}

}
