package com.agent.server.config;

import com.agent.server.model.entity.SysUser;
import com.agent.server.model.entity.SysUserRole;
import com.agent.server.repository.SysUserRepository;
import com.agent.server.repository.SysUserRoleRepository;
import com.agent.server.skill.SkillRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SysUserRepository userRepository;
    private final SysUserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SkillRegistry skillRegistry;

    @Override
    public void run(ApplicationArguments args) {
        // Create default admin user if no users exist
        if (userRepository.count() == 0) {
            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setNickname("管理员");
            admin.setAuthProvider("LOCAL");
            admin = userRepository.save(admin);

            SysUserRole role = new SysUserRole();
            role.setUserId(admin.getId());
            role.setRole("ADMIN");
            roleRepository.save(role);

            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(admin.getId());
            userRole.setRole("USER");
            roleRepository.save(userRole);

            log.info("Default admin user created: admin / admin123");
        }

        // Initialize built-in skills
        skillRegistry.initBuiltinSkills();
    }
}
