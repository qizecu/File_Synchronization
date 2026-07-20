package com.example.syncmanager.config;

import com.example.syncmanager.entity.SysUser;
import com.example.syncmanager.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化：插入默认管理员账号 admin / admin123
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin")
        );
        if (count == 0) {
            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("管理员");
            admin.setStatus(1);
            admin.setRole("ADMIN"); // 默认管理员角色为 ADMIN
            sysUserMapper.insert(admin);
            log.info("默认管理员账号已创建: admin / admin123");
        }
    }
}
