package com.mkt.ruleengine.web.controller;

import com.mkt.ruleengine.application.service.EventAppService;
import com.mkt.ruleengine.core.event.EventDefinition;
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
 * 事件管理 API：维护触发事件与入参字段定义。
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventAppService eventAppService;

    public EventController(EventAppService eventAppService) {
        this.eventAppService = eventAppService;
    }

    @GetMapping
    public ApiResponse<List<EventDefinition>> list() {
        return ApiResponse.ok(eventAppService.list());
    }

    @GetMapping("/{eventCode}")
    public ApiResponse<EventDefinition> get(@PathVariable String eventCode) {
        return ApiResponse.ok(eventAppService.get(eventCode));
    }

    @PostMapping
    public ApiResponse<EventDefinition> create(@RequestBody EventDefinition definition) {
        return ApiResponse.ok(eventAppService.create(definition));
    }

    @PutMapping("/{eventCode}")
    public ApiResponse<EventDefinition> update(@PathVariable String eventCode,
                                               @RequestBody EventDefinition definition) {
        return ApiResponse.ok(eventAppService.update(eventCode, definition));
    }

    @DeleteMapping("/{eventCode}")
    public ApiResponse<Void> delete(@PathVariable String eventCode) {
        eventAppService.delete(eventCode);
        return ApiResponse.ok();
    }

    @PostMapping("/{eventCode}/enable")
    public ApiResponse<EventDefinition> toggleEnabled(@PathVariable String eventCode,
                                                      @RequestParam boolean enabled) {
        return ApiResponse.ok(eventAppService.toggleEnabled(eventCode, enabled));
    }
}
