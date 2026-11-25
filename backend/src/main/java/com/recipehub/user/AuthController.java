package com.recipehub.user;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.user.dto.EmailRequest;
import com.recipehub.user.dto.LoginRequest;
import com.recipehub.user.dto.MeResponse;
import com.recipehub.user.dto.RegisterRequest;
import com.recipehub.user.dto.ResetPasswordRequest;
import com.recipehub.user.dto.TokenRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;

    public AuthController(AuthService auth, AuthenticationManager authenticationManager,
                          SecurityContextRepository contextRepository) {
        this.auth = auth;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
    }

    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void csrf(CsrfToken token) {
        token.getToken(); // đọc token để CsrfFilter ghi cookie XSRF-TOKEN
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest r) {
        auth.register(r);
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody TokenRequest r) {
        auth.verifyEmail(r.token());
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerification(@Valid @RequestBody EmailRequest r) {
        auth.resendVerification(r.email());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody EmailRequest r) {
        auth.forgotPassword(r.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest r) {
        auth.resetPassword(r.token(), r.newPassword());
    }

    @PostMapping("/login")
    public MeResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest request,
                            HttpServletResponse response) {
        Authentication result;
        try {
            result = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(AuthService.normalizeEmail(r.email()),
                            r.password()));
        } catch (DisabledException e) {
            throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED, "Email chưa được xác minh");
        } catch (LockedException e) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED, "Tài khoản đã bị khóa");
        } catch (AuthenticationException e) {
            throw new ApiException(ErrorCode.BAD_CREDENTIALS, "Email hoặc mật khẩu không đúng");
        }
        if (request.getSession(false) != null) {
            request.changeSessionId(); // chống session fixation
        }
        // Session chỉ giữ principal không có hash mật khẩu.
        AppUserDetails principal = ((AppUserDetails) result.getPrincipal()).withoutPassword();
        SecurityContext context = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        SecurityContextHolder.getContextHolderStrategy().setContext(context);
        contextRepository.saveContext(context, request, response);
        return auth.me(principal.id());
    }
}
