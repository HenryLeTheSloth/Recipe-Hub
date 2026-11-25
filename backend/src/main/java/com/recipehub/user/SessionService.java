package com.recipehub.user;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public SessionService(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    /** Xóa mọi phiên đăng nhập của user (principal name = email). */
    public void invalidateAll(String email) {
        invalidateOthers(email, null);
    }

    /** Như invalidateAll nhưng giữ lại phiên keepId (phiên đang thao tác). */
    public void invalidateOthers(String email, String keepId) {
        sessions.findByPrincipalName(email).keySet().stream()
                .filter(id -> !id.equals(keepId))
                .forEach(sessions::deleteById);
    }
}
