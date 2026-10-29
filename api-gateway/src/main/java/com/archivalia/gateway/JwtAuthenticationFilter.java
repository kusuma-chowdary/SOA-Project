package com.archivalia.gateway;

import com.archivalia.common.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtAuthenticationFilter implements GlobalFilter {
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }
        if (path.startsWith("/internal/")) {
            return reject(exchange, HttpStatus.NOT_FOUND, "Route not found.");
        }
        if (isPublicCatalogRead(path, method) || isPublicAuthAction(path, method)) {
            return chain.filter(exchange);
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "A valid bearer token is required.");
        }

        try {
            Claims claims = jwtService.parse(authorization.substring(7));
            String role = claims.get("role", String.class);
            if (isCatalogWrite(path, method) && !"ADMIN".equals(role)) {
                return reject(exchange, HttpStatus.FORBIDDEN, "Administrator access is required.");
            }
            ServerHttpRequest authenticatedRequest = request.mutate()
                    .headers(headers -> {
                        headers.remove("X-Authenticated-User");
                        headers.remove("X-Authenticated-Role");
                        headers.set("X-Authenticated-User", claims.getSubject());
                        headers.set("X-Authenticated-Role", role);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(authenticatedRequest).build());
        } catch (JwtException | IllegalArgumentException exception) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "The access token is invalid or expired.");
        }
    }

    private boolean isPublicCatalogRead(String path, HttpMethod method) {
        return HttpMethod.GET.equals(method) && ("/api/books".equals(path) || path.startsWith("/api/books/"));
    }

    private boolean isPublicAuthAction(String path, HttpMethod method) {
        return HttpMethod.POST.equals(method)
                && ("/api/auth/login".equals(path) || "/api/auth/register".equals(path));
    }

    private boolean isCatalogWrite(String path, HttpMethod method) {
        return path.equals("/api/books") || path.startsWith("/api/books/")
                ? !HttpMethod.GET.equals(method)
                : false;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
