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
        sessions.findByPrincipalName(email).keySet().forEach(sessions::deleteById);
    }
}
