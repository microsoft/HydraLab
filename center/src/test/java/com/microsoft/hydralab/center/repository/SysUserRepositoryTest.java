package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.center.repository.SysUserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SysUserRepositoryTest {

    @Mock
    private SysUserRepository sysUserRepository;

    @InjectMocks
    private SysUserRepository sysUserRepositoryUnderTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCountByRoleId() {
        String roleId = "testRoleId";
        int expectedCount = 5;
        when(sysUserRepository.countByRoleId(roleId)).thenReturn(expectedCount);
        int actualCount = sysUserRepositoryUnderTest.countByRoleId(roleId);
        assertEquals(expectedCount, actualCount);
    }
}
