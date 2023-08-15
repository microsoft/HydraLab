package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.center.repository.DeviceGroupRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeviceGroupRepositoryTest {

    @Mock
    private DeviceGroupRepository deviceGroupRepository;

    @InjectMocks
    private DeviceGroupRepository deviceGroupRepositoryUnderTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCountByGroupName() {
        String groupName = "Test Group";
        int expectedCount = 5;
        when(deviceGroupRepository.countByGroupName(groupName)).thenReturn(expectedCount);
        int actualCount = deviceGroupRepositoryUnderTest.countByGroupName(groupName);
        assertEquals(expectedCount, actualCount);
    }
}