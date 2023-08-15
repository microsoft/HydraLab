{
  "UT-File": [
    "package com.microsoft.hydralab.center.config;",
    "import org.springframework.context.annotation.Configuration;",
    "import org.springframework.context.annotation.Profile;",
    "import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;",
    "import org.springframework.security.config.annotation.web.builders.HttpSecurity;",
    "import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;",
    "import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;",
    "import org.mockito.Mockito;",
    "import org.junit.Test;",
    "",
    "public class AADOauth2ConfigurationTest {",
    "",
    "    @Test",
    "    public void testConfigure() throws Exception {",
    "        // Arrange",
    "        AADOauth2Configuration configuration = new AADOauth2Configuration();",
    "        HttpSecurity http = Mockito.mock(HttpSecurity.class);",
    "",
    "        // Act",
    "        configuration.configure(http);",
    "",
    "        // Assert",
    "        // Add your assertions here",
    "    }",
    "}"
  ]
}