package com.superpower;

import com.superpower.modules.system.entity.SysRole;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.repository.SysRoleRepository;
import com.superpower.modules.system.repository.SysUserRepository;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class SuperPowerApplication implements CommandLineRunner {

    private final SysRoleRepository roleRepository;
    private final SysUserRepository userRepository;
    private final DataVersionRepository versionRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperPowerApplication(SysRoleRepository roleRepository,
                                  SysUserRepository userRepository,
                                  DataVersionRepository versionRepository,
                                  PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.versionRepository = versionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static void main(String[] args) {
        SpringApplication.run(SuperPowerApplication.class, args);
    }

    @Override
    public void run(String... args) {
        if (roleRepository.count() > 0) return;

        SysRole adminRole = new SysRole();
        adminRole.setName("管理员");
        adminRole.setCode("ADMIN");
        adminRole.setDescription("系统管理员");
        roleRepository.save(adminRole);

        SysRole userRole = new SysRole();
        userRole.setName("普通用户");
        userRole.setCode("USER");
        userRole.setDescription("数据维护人员");
        roleRepository.save(userRole);

        SysRole advancedRole = new SysRole();
        advancedRole.setName("高级用户");
        advancedRole.setCode("ADVANCED");
        advancedRole.setDescription("高级用户，可查询和生成文档");
        roleRepository.save(advancedRole);

        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setNickname("管理员");
        admin.setRole(adminRole);
        admin.setStatus(1);
        userRepository.save(admin);

        SysUser user1 = new SysUser();
        user1.setUsername("user");
        user1.setPassword(passwordEncoder.encode("123456"));
        user1.setNickname("普通用户");
        user1.setRole(userRole);
        user1.setStatus(1);
        userRepository.save(user1);

        SysUser advanced = new SysUser();
        advanced.setUsername("advanced");
        advanced.setPassword(passwordEncoder.encode("123456"));
        advanced.setNickname("高级用户");
        advanced.setRole(advancedRole);
        advanced.setStatus(1);
        userRepository.save(advanced);

        DataVersion version = new DataVersion();
        version.setVersionNo("1.0");
        version.setStatus("draft");
        versionRepository.save(version);
    }
}
