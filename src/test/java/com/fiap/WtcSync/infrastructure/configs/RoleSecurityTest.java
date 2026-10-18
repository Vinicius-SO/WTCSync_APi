package com.fiap.WtcSync.infrastructure.configs;

import com.fiap.WtcSync.application.services.TokenService;
import com.fiap.WtcSync.domain.interfaces.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringJUnitConfig(RoleSecurityTest.Config.class)
@WebAppConfiguration
class RoleSecurityTest {
    @Configuration @EnableWebMvc @Import(SecurityConfig.class)
    static class Config {
        @Bean JwtAuthenticationFilter jwt() { return new JwtAuthenticationFilter(mock(TokenService.class), mock(IUserRepository.class)); }
        @Bean Endpoints endpoints() { return new Endpoints(); }
    }
    @RestController static class Endpoints {
        @GetMapping({"/api/clients", "/api/clients/me", "/api/campaigns", "/api/segments", "/messages/123", "/ws/info"})
        String ok() { return "ok"; }
    }
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    @BeforeEach void setup() { mvc = webAppContextSetup(context).apply(springSecurity()).build(); }
    @Test void anonymousCannotReadCrmOrMessages() throws Exception {
        mvc.perform(get("/api/clients")).andExpect(status().isForbidden());
        mvc.perform(get("/messages/123")).andExpect(status().isForbidden());
    }
    @Test void clientCannotReadCrmCampaignsOrSegments() throws Exception {
        for (String path : new String[]{"/api/clients", "/api/campaigns", "/api/segments", "/ws/info"})
            mvc.perform(get(path).with(user("client").authorities(new SimpleGrantedAuthority("CLIENT"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/clients/me").with(user("client").authorities(new SimpleGrantedAuthority("CLIENT"))))
            .andExpect(status().isOk());
    }
    @Test void operatorCanManageAndAttendantCannotSeeCampaigns() throws Exception {
        for (String path : new String[]{"/api/clients", "/api/campaigns", "/api/segments"})
            mvc.perform(get(path).with(user("operator").authorities(new SimpleGrantedAuthority("OPERATOR"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/campaigns").with(user("attendant").authorities(new SimpleGrantedAuthority("ATENDENTE"))))
            .andExpect(status().isForbidden());
    }
}
