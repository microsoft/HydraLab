package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.util.Const;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RunWith(MockitoJUnitRunner.class)
public class StorageTokenManageServiceTest {

    @Mock
    private StorageServiceClientProxy storageServiceClientProxy;
    private StorageTokenManageService storageTokenManageService;
    private ConcurrentMap<String, AccessToken> accessTokenMap;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        storageTokenManageService = new StorageTokenManageService();
        storageTokenManageService.storageServiceClientProxy = storageServiceClientProxy;
        accessTokenMap = new ConcurrentHashMap<>();
    }

    @Test
    public void testGenerateReadToken() {
        String uniqueId = "123";
        AccessToken accessToken = new AccessToken();
        accessTokenMap.put(uniqueId, accessToken);
        AccessToken result = storageTokenManageService.generateReadToken(uniqueId);
        Assert.assertEquals(accessToken, result);
    }

    @Test
    public void testGenerateWriteToken() {
        String uniqueId = "testUniqueId";
        AccessToken accessToken = new AccessToken();
        accessTokenMap.put(uniqueId, accessToken);
        Mockito.when(storageServiceClientProxy.isAccessTokenExpired(accessToken)).thenReturn(false);
        Mockito.when(storageServiceClientProxy.generateAccessToken(Const.FilePermission.WRITE)).thenReturn(accessToken);
        AccessToken result = storageTokenManageService.generateWriteToken(uniqueId);
        Assert.assertNotNull(result);
        Assert.assertEquals(accessToken, result);
    }

    @Test
    public void testValidateAccessToken() {
        String accessToken = "validAccessToken";
        boolean expected = true;
        boolean actual = storageTokenManageService.validateAccessToken(accessToken);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testValidateTokenVal() {
        String token = "exampleToken";
        boolean expectedResult = true;
        boolean actualResult = storageTokenManageService.validateTokenVal(token);
        Assert.assertEquals(expectedResult, actualResult);
    }
}