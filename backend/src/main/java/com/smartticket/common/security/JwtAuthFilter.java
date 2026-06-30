package com.smartticket.common.security;

import com.smartticket.common.context.UserContext;
import com.smartticket.common.util.RedisUtil;
import com.smartticket.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器（B4）：解析 token → 校验黑名单 → 注入权限码到 SecurityContext，
 * 并写 UserContext 供审计/业务读取。token 无效则放行给后续鉴权拒绝（401）。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final UserService userService;

    public JwtAuthFilter(JwtUtil jwtUtil, RedisUtil redisUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.redisUtil = redisUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && !Boolean.TRUE.equals(redisUtil.hasKey(BLACKLIST_PREFIX + token))) {
            try {
                Claims claims = jwtUtil.parse(token);
                Long uid = claims.get("uid", Long.class);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                List<String> perms = userService.getPermCodes(roleIdFromRole(role, uid));
                var authorities = perms.stream().map(SimpleGrantedAuthority::new).toList();

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                UserContext.set(new UserContext.CurrentUser(uid, username, role));
            } catch (Exception ignored) {
                // 无效 token：不设置认证，交由后续鉴权返回 401
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /** role 码 -> roleId（与种子一致 EMPLOYEE=1/ENGINEER=2/ADMIN=3）。 */
    private Long roleIdFromRole(String role, Long uid) {
        return switch (role == null ? "" : role) {
            case "EMPLOYEE" -> 1L;
            case "ENGINEER" -> 2L;
            case "ADMIN" -> 3L;
            default -> -1L;
        };
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
