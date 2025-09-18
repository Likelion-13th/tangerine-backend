package likelion13th.shop.login.auth.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
// reissue 재요청이 왔을때...
public class AuthCreationFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request){
        return !"/users/reissue".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated()
            && !(existing instanceof AnonymousAuthenticationToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring("Bearer ".length());

        String providerId = null;
        try {
            Claims claims = tokenProvider.parseClaimsAllowExpired(token);
            providerId = claims.getSubject();
        } catch(Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        if (providerId == null || providerId.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 익명 권한 부여
        var anonymousAuthorities = java.util.Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS")
        );

        var preAuth = new org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken(
                providerId, "N/A", anonymousAuthorities
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(preAuth);
        SecurityContextHolder.setContext(context);

        filterChain.doFilter(request, response);
    }
}
