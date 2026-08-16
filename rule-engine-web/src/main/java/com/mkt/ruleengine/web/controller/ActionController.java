package com.mkt.ruleengine.web.controller;

import com.mkt.ruleengine.application.service.ActionAppService;
import com.mkt.ruleengine.core.action.ActionDefinition;
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

import java.util.List;

/**
 * 动作配置 API：动作模板（券模板 / 短信模板 / 积分数量）CRUD。
 */
@RestController
@RequestMapping("/api/actions")
public class ActionController {

    private final ActionAppService actionAppService;

    public ActionController(ActionAppService actionAppService) {
        this.actionAppService = actionAppService;
    }

    @GetMapping
    public ApiResponse<List<ActionDefinition>> list() {
        return ApiResponse.ok(actionAppService.list());
    }

    @GetMapping("/{actionCode}")
    public ApiResponse<ActionDefinition> get(@PathVariable String actionCode) {
        return ApiResponse.ok(actionAppService.get(actionCode));
    }

    @PostMapping
    public ApiResponse<ActionDefinition> create(@RequestBody ActionDefinition definition) {
        return ApiResponse.ok(actionAppService.create(definition));
    }

    @PutMapping("/{actionCode}")
    public ApiResponse<ActionDefinition> update(@PathVariable String actionCode,
                                                @RequestBody ActionDefinition definition) {
        return ApiResponse.ok(actionAppService.update(actionCode, definition));
    }

    @DeleteMapping("/{actionCode}")
    public ApiResponse<Void> delete(@PathVariable String actionCode) {
        actionAppService.delete(actionCode);
        return ApiResponse.ok();
    }

    @PostMapping("/{actionCode}/enable")
    public ApiResponse<ActionDefinition> toggleEnabled(@PathVariable String actionCode,
                                                       @RequestParam boolean enabled) {
        return ApiResponse.ok(actionAppService.toggleEnabled(actionCode, enabled));
    }
}
