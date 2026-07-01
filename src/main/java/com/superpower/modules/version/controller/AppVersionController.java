package com.superpower.modules.version.controller;

import com.superpower.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class AppVersionController {

    @GetMapping("/api/app-version")
    public Result<String> getAppVersion() {
        try {
            Path versionFile = Paths.get("VERSION.md");
            if (Files.exists(versionFile)) {
                String firstLine = Files.lines(versionFile)
                        .filter(line -> !line.trim().isEmpty())
                        .findFirst()
                        .orElse("");
                String version = firstLine.replaceAll("^#+\\s*", "").trim();
                if (version.startsWith("当前研发版本:") || version.startsWith("当前研发版本：")) {
                    version = version.replaceAll("^当前研发版本[:：]\\s*", "").trim();
                }
                return Result.success(version);
            }
            return Result.success("unknown");
        } catch (IOException e) {
            return Result.success("unknown");
        }
    }
}