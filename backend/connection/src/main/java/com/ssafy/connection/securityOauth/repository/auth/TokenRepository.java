package com.ssafy.connection.securityOauth.repository.auth;

import com.ssafy.connection.securityOauth.domain.entity.user.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByGithubId(String githubId);
    Optional<Token> findByRefreshToken(String refreshToken);
}