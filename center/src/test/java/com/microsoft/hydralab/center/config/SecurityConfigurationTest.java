package com.microsoft.hydralab.center.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;

@RunWith(MockitoJUnitRunner.class)
public class SecurityConfigurationTest {

    @Mock
    private InMemoryUserDetailsManager userDetailsManager;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Test
    public void testUserDetailsService() {
        Mockito.when(userDetailsManager.loadUserByUsername(Mockito.anyString())).thenReturn(null);
        SecurityConfiguration securityConfiguration = new SecurityConfiguration();
        UserDetailsService userDetailsService = securityConfiguration.userDetailsService();
    }

    @Test
    public void testPasswordEncoder() {
        PasswordEncoder passwordEncoder = Mockito.mock(BCryptPasswordEncoder.class);
        SecurityConfiguration securityConfiguration = new SecurityConfiguration();
        Mockito.when(passwordEncoder.encode(Mockito.anyString())).thenReturn("encodedPassword");
        PasswordEncoder result = securityConfiguration.passwordEncoder();
        Mockito.verify(passwordEncoder).encode(Mockito.anyString());
        Mockito.verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    public void testAuthenticationManagerBean() {
        SecurityConfiguration securityConfiguration = new SecurityConfiguration();
        Mockito.when(userDetailsManager.createUser(Mockito.any())).thenReturn(null);
        Mockito.when(userDetailsManager.loadUserByUsername(Mockito.anyString())).thenReturn(null);
        AuthenticationManager authenticationManager = securityConfiguration.authenticationManagerBean();
    }
}