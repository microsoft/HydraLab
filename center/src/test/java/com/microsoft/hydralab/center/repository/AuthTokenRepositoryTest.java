package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.common.entity.center.AuthToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class AuthTokenRepositoryTest {

    @InjectMocks
    private AuthTokenRepository authTokenRepository;

    @Mock
    private AuthTokenRepository authTokenRepositoryMock;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCountByToken() {
        String token = "testToken";
        List<AuthToken> authTokens = new ArrayList<>();
        authTokens.add(new AuthToken());
        Mockito.when(authTokenRepositoryMock.countByToken(token)).thenReturn(authTokens.size());
        int result = authTokenRepository.countByToken(token);
        assertEquals(authTokens.size(), result);
    }
}