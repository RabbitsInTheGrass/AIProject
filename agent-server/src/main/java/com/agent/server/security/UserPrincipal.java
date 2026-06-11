package com.agent.server.security;

import com.agent.server.model.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private Long id;
    private String username;
    private String password;
    private String email;
    private String nickname;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal from(SysUser user, List<String> roles) {
        List<GrantedAuthority> auths = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getEmail(),
                user.getNickname(),
                auths
        );
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}
