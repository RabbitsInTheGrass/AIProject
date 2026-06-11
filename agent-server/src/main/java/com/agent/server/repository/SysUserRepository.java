package com.agent.server.repository;

import com.agent.server.model.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUsername(String username);
    Optional<SysUser> findByEmail(String email);
    Optional<SysUser> findByPhone(String phone);
    Optional<SysUser> findByAuthProviderAndOauthId(String authProvider, String oauthId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
