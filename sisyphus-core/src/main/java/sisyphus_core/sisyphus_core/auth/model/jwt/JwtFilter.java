package sisyphus_core.sisyphus_core.auth.model.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import sisyphus_core.sisyphus_core.auth.model.CustomUserDetails;
import sisyphus_core.sisyphus_core.auth.model.Token;
import sisyphus_core.sisyphus_core.auth.service.TokenService;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        String token = null;

        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){
            token = authorizationHeader.substring(7);
        }
        log.info("token : {}", token);
        if (token != null) {
            //토큰없는상태 첫 로그인
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateToken(token,response);
            }else{
                if (jwtUtil.isExpired(token)) {
                    handleExpiredToken(token, request, response);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    //토큰없을때 첫 로그인하면 SecurityContextHolder에 정보 주입
    private void authenticateToken(String token, HttpServletResponse response) {
        if (!jwtUtil.isExpired(token)) {
            Authentication authentication = jwtUtil.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            log.info("AccessToken 만료됨");
            // AccessToken 만료 시 처리 로직 추가 가능
            Token token1 = tokenService.findTokenByToken(token);
            String loginId = jwtUtil.getLoginId(token1.getRefreshToken());
            if(!jwtUtil.isExpired(token1.getRefreshToken())){
                log.info("AccessToken 재발급됨");
                String newAccessToken = jwtUtil.getAccessToken((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
                token1.setAccessToken(newAccessToken);
                tokenService.saveToken(token1);
                response.setHeader("Authorization", "Bearer " + newAccessToken);
                SecurityContextHolder.getContext().setAuthentication(jwtUtil.getAuthentication(newAccessToken));
            }else {
                tokenService.deleteToken(loginId);
            }
        }
    }

    //SecurityContextHolder에 따라 토큰 검증
    private void handleExpiredToken(String token, HttpServletRequest request, HttpServletResponse response) {
        Token redisToken = tokenService.findTokenByToken(token);
        String loginId = jwtUtil.getLoginId(redisToken.getRefreshToken());

        if (redisToken != null && jwtUtil.isExpired(redisToken.getRefreshToken())) {
            // RefreshToken도 만료된 경우
            log.info("RefreshToken 만료");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);  // 403 Forbidden 응답
        } else if (redisToken != null) {
            // RefreshToken이 유효한 경우 AccessToken 재발급
            log.info("RefreshToken 유효");
            String newAccessToken = jwtUtil.getAccessToken((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            redisToken.setAccessToken(newAccessToken);
            tokenService.saveToken(redisToken);
            response.setHeader("Authorization", "Bearer " + newAccessToken);
            SecurityContextHolder.getContext().setAuthentication(jwtUtil.getAuthentication(newAccessToken));
        } else {
            tokenService.deleteToken(loginId);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);  // 403 Forbidden 응답
        }
    }
}
