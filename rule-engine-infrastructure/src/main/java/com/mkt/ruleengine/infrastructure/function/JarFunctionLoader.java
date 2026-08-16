package com.mkt.ruleengine.infrastructure.function;

import com.mkt.ruleengine.core.exception.FunctionLoadException;
import com.mkt.ruleengine.core.function.FunctionDefinition;
import com.mkt.ruleengine.core.function.FunctionType;
import com.mkt.ruleengine.core.function.MarketingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Jar 函数加载器：上传 Jar 包后通过独立 URLClassLoader 动态加载实现类（热更新支持）。
 * 类加载器按 (jarPath + className + version) 缓存，同一版本复用，新版本创建新 ClassLoader。
 */
@Component
public class JarFunctionLoader implements FunctionLoader {

    private static final Logger log = LoggerFactory.getLogger(JarFunctionLoader.class);

    private final ConcurrentMap<String, MarketingFunction> loaded = new ConcurrentHashMap<>();

    @Override
    public boolean supports(FunctionType type) {
        return type == FunctionType.JAR;
    }

    @Override
    public MarketingFunction load(FunctionDefinition definition) {
        String key = definition.getJarPath() + "|" + definition.getClassName() + "|v" + definition.getVersion();
        return loaded.computeIfAbsent(key, k -> doLoad(definition));
    }

    private MarketingFunction doLoad(FunctionDefinition definition) {
        try {
            File jarFile = new File(definition.getJarPath());
            if (!jarFile.exists()) {
                throw new FunctionLoadException("jar not found: " + definition.getJarPath());
            }
            // 父加载器用当前类加载器，保证 MarketingFunction 接口一致
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader());
            Class<?> clazz = Class.forName(definition.getClassName(), true, classLoader);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (!(instance instanceof MarketingFunction function)) {
                throw new FunctionLoadException("class does not implement MarketingFunction: " + definition.getClassName());
            }
            log.info("jar function loaded: {} -> {}", definition.getFunctionName(), definition.getClassName());
            return function;
        } catch (FunctionLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new FunctionLoadException("load jar function failed: " + definition.getFunctionName(), e);
        }
    }
}
