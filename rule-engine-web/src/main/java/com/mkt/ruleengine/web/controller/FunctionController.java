package com.mkt.ruleengine.web.controller;

import com.mkt.ruleengine.application.service.FunctionAppService;
import com.mkt.ruleengine.core.action.ActionExecutorFactory;
import com.mkt.ruleengine.core.function.FunctionDefinition;
import com.mkt.ruleengine.infrastructure.config.RuleEngineProperties;
import com.mkt.ruleengine.web.common.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 自定义函数注册 API：Jar 上传 / 在线脚本 / 测试运行。
 */
@RestController
@RequestMapping("/api/functions")
public class FunctionController {

    private final FunctionAppService functionAppService;
    private final ActionExecutorFactory actionExecutorFactory;
    private final RuleEngineProperties properties;

    public FunctionController(FunctionAppService functionAppService,
                              ActionExecutorFactory actionExecutorFactory,
                              RuleEngineProperties properties) {
        this.functionAppService = functionAppService;
        this.actionExecutorFactory = actionExecutorFactory;
        this.properties = properties;
    }

    @GetMapping
    public ApiResponse<List<FunctionDefinition>> list() {
        return ApiResponse.ok(functionAppService.list());
    }

    @GetMapping("/{functionName}")
    public ApiResponse<FunctionDefinition> get(@PathVariable String functionName) {
        return ApiResponse.ok(functionAppService.get(functionName));
    }

    @PostMapping
    public ApiResponse<FunctionDefinition> register(@RequestBody FunctionDefinition definition) {
        return ApiResponse.ok(functionAppService.register(definition));
    }

    @PutMapping("/{functionName}")
    public ApiResponse<FunctionDefinition> update(@PathVariable String functionName,
                                                  @RequestBody FunctionDefinition definition) {
        return ApiResponse.ok(functionAppService.update(functionName, definition));
    }

    @DeleteMapping("/{functionName}")
    public ApiResponse<Void> delete(@PathVariable String functionName) {
        functionAppService.delete(functionName);
        return ApiResponse.ok();
    }

    @PostMapping("/{functionName}/enable")
    public ApiResponse<FunctionDefinition> toggleEnabled(@PathVariable String functionName,
                                                         @RequestParam boolean enabled) {
        return ApiResponse.ok(functionAppService.toggleEnabled(functionName, enabled));
    }

    /** 在线测试函数 */
    @PostMapping("/{functionName}/test")
    public ApiResponse<Object> test(@PathVariable String functionName,
                                    @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> eventParams = body == null ? Map.of() : (Map<String, Object>) body.getOrDefault("eventParams", Map.of());
        Map<String, Object> bindings = body == null ? Map.of() : (Map<String, Object>) body.getOrDefault("bindings", Map.of());
        return ApiResponse.ok(functionAppService.testRun(functionName, eventParams, bindings));
    }

    /** 上传函数 Jar 并注册 */
    @PostMapping("/upload-jar")
    public ApiResponse<FunctionDefinition> uploadJar(@RequestParam("file") MultipartFile file,
                                                     @RequestParam String functionName,
                                                     @RequestParam String className,
                                                     @RequestParam(required = false) String displayName,
                                                     @RequestParam(required = false) String description) {
        FunctionDefinition def = new FunctionDefinition();
        def.setFunctionName(functionName);
        def.setDisplayName(displayName == null ? functionName : displayName);
        def.setDescription(description);
        def.setType(com.mkt.ruleengine.core.function.FunctionType.JAR);
        def.setClassName(className);
        def.setJarPath(saveJarFile(file, functionName));
        return ApiResponse.ok(functionAppService.register(def));
    }

    private String saveJarFile(MultipartFile file, String functionName) {
        try {
            java.io.File dir = new java.io.File(properties.functionJarDir());
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException("cannot create jar dir: " + dir.getAbsolutePath());
            }
            java.io.File target = new java.io.File(dir, functionName + "-" + System.currentTimeMillis() + ".jar");
            file.transferTo(target);
            return target.getAbsolutePath();
        } catch (Exception e) {
            throw new IllegalStateException("save jar failed: " + e.getMessage(), e);
        }
    }

    /** 已注册动作执行器类型（画布动作下拉选项） */
    @GetMapping("/executor-types")
    public ApiResponse<List<String>> executorTypes() {
        return ApiResponse.ok(actionExecutorFactory.all().keySet().stream().sorted().toList());
    }
}
