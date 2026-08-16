package com.mkt.ruleengine.web.controller;

import com.mkt.ruleengine.application.service.RuleConfigAppService;
import com.mkt.ruleengine.core.gray.GrayConfig;
import com.mkt.ruleengine.core.rule.RuleGroup;
import com.mkt.ruleengine.core.rule.RuleVersion;
import com.mkt.ruleengine.web.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 规则配置 API：画布配置 / 发布 / 版本回溯 / 灰度 / 上下线。
 */
@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleConfigAppService ruleConfigAppService;

    public RuleController(RuleConfigAppService ruleConfigAppService) {
        this.ruleConfigAppService = ruleConfigAppService;
    }

    @GetMapping
    public ApiResponse<List<RuleGroup>> list() {
        return ApiResponse.ok(ruleConfigAppService.listRules());
    }

    @GetMapping("/{ruleCode}")
    public ApiResponse<RuleGroup> get(@PathVariable String ruleCode) {
        return ApiResponse.ok(ruleConfigAppService.getRule(ruleCode));
    }

    /** 创建规则（含初始草稿版本） */
    @PostMapping
    public ApiResponse<RuleGroup> create(@RequestBody RuleGroup group) {
        return ApiResponse.ok(ruleConfigAppService.createRuleGroup(group));
    }

    /** 更新画布草稿 */
    @PutMapping("/{ruleCode}")
    public ApiResponse<RuleGroup> updateDraft(@PathVariable String ruleCode,
                                              @RequestBody RuleGroup group) {
        return ApiResponse.ok(ruleConfigAppService.updateDraft(ruleCode, group));
    }

    /** 发布为线上版本 */
    @PostMapping("/{ruleCode}/publish")
    public ApiResponse<RuleVersion> publish(@PathVariable String ruleCode,
                                            @RequestParam(required = false) String changeLog,
                                            @RequestParam(required = false, defaultValue = "console") String operator) {
        return ApiResponse.ok(ruleConfigAppService.publish(ruleCode, changeLog, operator));
    }

    /** 版本回溯 */
    @PostMapping("/versions/{versionId}/rollback")
    public ApiResponse<RuleVersion> rollback(@PathVariable Long versionId,
                                             @RequestParam(required = false) String changeLog,
                                             @RequestParam(required = false, defaultValue = "console") String operator) {
        return ApiResponse.ok(ruleConfigAppService.rollback(versionId, changeLog, operator));
    }

    /** 灰度配置（即时生效） */
    @PostMapping("/{ruleCode}/gray")
    public ApiResponse<RuleGroup> setGray(@PathVariable String ruleCode,
                                          @RequestBody GrayConfig gray) {
        return ApiResponse.ok(ruleConfigAppService.setGray(ruleCode, gray));
    }

    @PostMapping("/{ruleCode}/online")
    public ApiResponse<RuleGroup> online(@PathVariable String ruleCode) {
        return ApiResponse.ok(ruleConfigAppService.online(ruleCode));
    }

    @PostMapping("/{ruleCode}/offline")
    public ApiResponse<RuleGroup> offline(@PathVariable String ruleCode) {
        return ApiResponse.ok(ruleConfigAppService.offline(ruleCode));
    }

    @GetMapping("/{ruleCode}/versions")
    public ApiResponse<List<RuleVersion>> versions(@PathVariable String ruleCode) {
        return ApiResponse.ok(ruleConfigAppService.listVersions(ruleCode));
    }

    @GetMapping("/versions/{versionId}")
    public ApiResponse<RuleVersion> version(@PathVariable Long versionId) {
        return ApiResponse.ok(ruleConfigAppService.getVersion(versionId));
    }

    /** 历史版本内容（画布回显） */
    @GetMapping("/versions/{versionId}/content")
    public ApiResponse<RuleGroup> versionContent(@PathVariable Long versionId) {
        return ApiResponse.ok(ruleConfigAppService.groupFromVersion(versionId));
    }
}
