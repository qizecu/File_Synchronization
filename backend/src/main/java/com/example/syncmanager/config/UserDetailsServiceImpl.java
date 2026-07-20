package com.example.syncmanager.config;

import com.example.syncmanager.mapper.SysUserMapper;
import com.example.syncmanager.entity.SysUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 用户加载
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getStatus, 1)
        );
        if (sysUser == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        // 根据数据库角色映射 Spring Security 权限
        String role = "ADMIN".equals(sysUser.getRole()) ? "ROLE_ADMIN" : "ROLE_USER";
        return User.builder()
                .username(sysUser.getUsername())
                .password(sysUser.getPassword())
                .disabled(false)
                .accountExpired(false)
                .credentialsExpired(false)
                .accountLocked(false)
                .authorities(role)
                .build();
    }
}
