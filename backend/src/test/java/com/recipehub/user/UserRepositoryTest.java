package com.recipehub.user;

import com.recipehub.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PermissionRepository permissions;
    @Autowired EntityManager em;

    User user(String email, String username, String displayName, String roleCode) {
        User u = new User(email, username, "{noop}x", displayName);
        if (roleCode != null) u.getRoles().add(roles.findByCode(roleCode).orElseThrow());
        return users.saveAndFlush(u);
    }

    @Test
    void seedHasRolesAndPermissions() {
        assertThat(permissions.count()).isEqualTo(9);
        assertThat(roles.findByCode("MEMBER").orElseThrow().getPermissions()).extracting(Permission::getCode)
                .containsExactlyInAnyOrder("RECIPE_WRITE", "COMMUNITY_PARTICIPATE", "AI_BOWL_USE", "MEDIA_UPLOAD");
        assertThat(roles.findByCode("MODERATOR").orElseThrow().getPermissions()).hasSize(7);
        assertThat(roles.findByCode("ADMIN").orElseThrow().getPermissions()).hasSize(9);
        assertThat(roles.findAll()).allMatch(Role::isSystem);
    }

    @Test
    void loadsAuthoritiesWithUser() {
        user("mod@x.com", "mod", "Mod", "MODERATOR");
        User u = users.findWithAuthoritiesByEmail("mod@x.com").orElseThrow();
        assertThat(u.getRoles()).flatExtracting(Role::getPermissions).extracting(Permission::getCode)
                .contains("CONTENT_MODERATE", "USER_READ").doesNotContain("USER_MANAGE");
    }

    @Test
    void duplicateEmailRejectedByDb() {
        user("a@x.com", "a1", "A", null);
        assertThatThrownBy(() -> user("a@x.com", "a2", "A", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void usernameUniqueIgnoringCaseInDb() {
        user("u1@x.com", "Bep", "A", null);
        assertThatThrownBy(() -> user("u2@x.com", "bep", "B", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void roleInUseCannotBeDeleted() {
        Role r = new Role("COOK", "Đầu bếp", null);
        roles.saveAndFlush(r);
        User u = new User("c@x.com", "cook", "{noop}x", "C");
        u.getRoles().add(r);
        users.saveAndFlush(u);
        em.clear(); // để DB (FK RESTRICT) từ chối, không phải kiểm tra trong bộ nhớ của Hibernate
        assertThatThrownBy(() -> {
            roles.deleteById(r.getId());
            roles.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void searchIsCaseInsensitive() {
        user("b@x.com", "bep", "Bếp Trưởng", null);
        user("d@x.com", "dau", "Đầu Bếp Phụ", null);
        assertThat(users.search("bếp trưởng", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(users.search("BẾP", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);
        assertThat(users.search("", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);
        assertThat(users.countByRoles_Code("ADMIN")).isZero();
    }
}
