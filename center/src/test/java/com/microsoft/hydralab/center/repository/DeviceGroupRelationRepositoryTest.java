package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.center.repository.DeviceGroupRelationRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeviceGroupRelationRepositoryTest {

    @Mock
    private DeviceGroupRelationRepository deviceGroupRelationRepository;

    @InjectMocks
    private DeviceGroupRelationService deviceGroupRelationService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDeleteAllByGroupName() {
        String groupName = "TestGroup";
        List<DeviceGroupRelation> deviceGroupRelations = new ArrayList<>();
        when(deviceGroupRelationRepository.findAllByGroupName(groupName)).thenReturn(deviceGroupRelations);
        deviceGroupRelationService.deleteAllByGroupName(groupName);
        verify(deviceGroupRelationRepository).deleteAllByGroupName(groupName);
    }
}