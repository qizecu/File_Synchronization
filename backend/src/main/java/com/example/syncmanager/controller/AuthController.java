package com.example.syncmanager.controller;

import com.example.syncmanager.common.BusinessException;
import com.example.syncmanager.common.Result;
import com.example.syncmanager.config.JwtUtil;
import com.example.syncmanager.dto.LoginDTO;
import com.example.syncmanager.dto.LoginVO;
import com.example.syncmanager.entity.SysUser;
import com.example.syncmanager.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        // Spring Security 认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );

        // 从数据库查询完整用户信息（含昵称、角色）
        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername())
        );

        // 生成 JWT Token，携带角色信息
        String token = jwtUtil.generateToken(sysUser.getId(), sysUser.getUsername(), sysUser.getRole());

        LoginVO vo = LoginVO.builder()
                .token(token)
                .username(sysUser.getUsername())
                .nickname(sysUser.getNickname())
                .role(sysUser.getRole())
                .build();
        return Result.success(vo);
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/me")
    public Result<LoginVO> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }
        // 从数据库查询完整用户信息
        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, auth.getName())
        );
        if (sysUser == null) {
            throw new BusinessException(401, "用户不存在");
        }
        LoginVO vo = LoginVO.builder()
                .username(sysUser.getUsername())
                .nickname(sysUser.getNickname())
                .role(sysUser.getRole())
                .build();
        return Result.success(vo);
    }

    /** 登出（前端清除 Token 即可，后端无状态） */
    @PostMapping("/logout")
    public Result<Void> logout() {
        SecurityContextHolder.clearContext();
        return Result.success();
    }
}
