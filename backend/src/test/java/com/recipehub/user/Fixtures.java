package com.recipehub.user;

import java.util.Set;
import org.springframework.test.util.ReflectionTestUtils;

/** Entity có id cho test Mockito (id do DB sinh nên không có setter). */
final class Fixtures {

    private Fixtures() {
    }

    static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    static Role role(long id, String code, String... perms) {
        Role r = withId(new Role(code, code, null), id);
        for (String p : perms) {
            r.getPermissions().add(new Permission(p, p));
        }
        return r;
    }

    static User user(long id, String email, Role... roles) {
        User u = withId(new User(email, "u" + id, "HASH", "U" + id), id);
        u.getRoles().addAll(Set.of(roles));
        return u;
    }

    static AppUserDetails actor(long id) {
        return new AppUserDetails(id, "actor@x.com", "HASH", true, false, Set.of());
    }
}
