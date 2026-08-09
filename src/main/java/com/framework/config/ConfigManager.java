package com.framework.config;

import com.framework.exceptions.FrameworkConfigException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;


public final class ConfigManager {

    private static volatile ConfigManager instance;

    private final Properties properties = new Properties();

    private ConfigManager() {
        loadResource("config/config.properties", true);
        String env = System.getProperty("env", System.getenv().getOrDefault("ENV", "qa"));
        loadResource("config/" + env.toLowerCase(Locale.ROOT) + ".properties", false);
    }

    public static ConfigManager getInstance() {
        ConfigManager result = instance;
        if (result == null) {
            synchronized (ConfigManager.class) {
                result = instance;
                if (result == null) {
                    instance = result = new ConfigManager();
                }
            }
        }
        return result;
    }

    private void loadResource(String classpathLocation, boolean required) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathLocation)) {
            if (in == null) {
                if (required) {
                    throw new FrameworkConfigException("Required config resource not found on classpath: " + classpathLocation);
                }
                return;
            }
            properties.load(in);
        } catch (IOException e) {
            throw new FrameworkConfigException("Failed to load config resource: " + classpathLocation, e);
        }
    }

    public String get(String key) {
        return get(key, null);
    }

    public String get(String key, String defaultValue) {
        String systemProperty = System.getProperty(key);
        if (systemProperty != null) {
            return systemProperty;
        }
        String envVar = System.getenv(toEnvVarName(key));
        if (envVar != null) {
            return envVar;
        }
        return properties.getProperty(key, defaultValue);
    }

    public String getRequired(String key) {
        String value = get(key);
        if (value == null || value.isBlank()) {
            throw new FrameworkConfigException("Missing required config key: " + key);
        }
        return value;
    }

    public int getInt(String key, int defaultValue) {
        String value = get(key);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value.trim());
    }

    public long getLong(String key, long defaultValue) {
        String value = get(key);
        return value == null || value.isBlank() ? defaultValue : Long.parseLong(value.trim());
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value == null || value.isBlank() ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    private static String toEnvVarName(String key) {
        return key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
    }
}
