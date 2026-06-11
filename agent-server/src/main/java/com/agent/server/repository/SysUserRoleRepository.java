package com.agent.server.repository;

import com.agent.server.model.entity.SysUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysUserRoleRepository extends JpaRepository<SysUserRole, Long> {
    List<SysUserRole> findByUserId(Long userId);
}
