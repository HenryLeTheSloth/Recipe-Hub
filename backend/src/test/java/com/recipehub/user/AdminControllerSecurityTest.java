package com.recipehub.user;

import com.recipehub.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest({AdminUserController.class, AdminRoleController.class})
@Import(SecurityConfig.class)
class AdminControllerSecurityTest {

    @Autowired MockMvcTester mvc;
    @MockitoBean AdminUserService adminUsers;
    @MockitoBean RoleService roleService;

    @Test
    void anonymousIs401() {
        assertThat(mvc.get().uri("/api/admin/users").exchange()).hasStatus(401);
        assertThat(mvc.get().uri("/api/admin/roles").exchange()).hasStatus(401);
    }

    @Test
    @WithMockUser(authorities = "RECIPE_WRITE")
    void memberIs403() {
        assertThat(mvc.get().uri("/api/admin/users").exchange()).hasStatus(403);
    }

    @Test
    @WithMockUser(authorities = "USER_READ")
    void readerCanListButNotLock() {
        when(adminUsers.list(any(), any())).thenReturn(Page.empty());
        assertThat(mvc.get().uri("/api/admin/users").exchange()).hasStatusOk();
        assertThat(mvc.patch().uri("/api/admin/users/5/lock").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"locked\":true}").exchange()).hasStatus(403);
    }

    @Test
    @WithMockUser(authorities = "USER_MANAGE")
    void managerCannotTouchRoles() {
        assertThat(mvc.get().uri("/api/admin/roles").exchange()).hasStatus(403);
        assertThat(mvc.get().uri("/api/admin/permissions").exchange()).hasStatus(403);
    }

    @Test
    @WithMockUser(authorities = "ROLE_MANAGE")
    void roleManagerCanListRolesAndPermissions() {
        assertThat(mvc.get().uri("/api/admin/roles").exchange()).hasStatusOk();
        assertThat(mvc.get().uri("/api/admin/permissions").exchange()).hasStatusOk();
    }

    @Test
    @WithMockUser(authorities = "ROLE_MANAGE")
    void invalidRoleCodeIs400() {
        assertThat(mvc.post().uri("/api/admin/roles").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"bad code\",\"name\":\"X\",\"permissionCodes\":[]}").exchange())
                .hasStatus(400).bodyJson().extractingPath("$.errors.code").isNotNull();
    }
}
