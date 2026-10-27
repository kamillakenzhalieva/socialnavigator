package com.social_navigator_ar.demo.parser;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "parser")
public class ParserProperties {

    private boolean demoMode = false;
    private int minScore = 50;
    private int requestDelayMs = 1500;
    private int maxProfilesPerRun = 40;

    private final Yandex yandex = new Yandex();
    private final Profi profi = new Profi();
    private final Hh hh = new Hh();

    public static class Yandex {
        private String apiKey = "";
        private String folderId = "";
        private String model = "yandexgpt-lite";
        private String modelVersion = "latest";
        private int maxCallsPerRun = 60;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getFolderId() { return folderId; }
        public void setFolderId(String folderId) { this.folderId = folderId; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getModelVersion() { return modelVersion; }
        public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
        public int getMaxCallsPerRun() { return maxCallsPerRun; }
        public void setMaxCallsPerRun(int maxCallsPerRun) { this.maxCallsPerRun = maxCallsPerRun; }
        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank()
                    && folderId != null && !folderId.isBlank();
        }
    }

    public static class Profi {
        private boolean enabled = true;

        private List<String> rootUrls = new ArrayList<>(List.of(
                "https://profi.ru/geo-astrahan/repetitor/psihologia/",
                "https://profi.ru/geo-astrahan/logoped/",
                "https://profi.ru/geo-astrahan/defektolog/"
        ));

        private String userAgent =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/131.0.0.0 Safari/537.36";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<String> getRootUrls() { return rootUrls; }
        public void setRootUrls(List<String> rootUrls) { this.rootUrls = rootUrls; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    }

    public static class Hh {
        private boolean enabled = false;
        private String areaId = "";
        private String userAgent = "SocialNavigator/1.0 (admin@example.com)";
        private String accessToken = "";
        private int periodDays = 3;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getAreaId() { return areaId; }
        public void setAreaId(String areaId) { this.areaId = areaId; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public int getPeriodDays() { return periodDays; }
        public void setPeriodDays(int periodDays) { this.periodDays = periodDays; }
    }

    public boolean isDemoMode() { return demoMode; }
    public void setDemoMode(boolean demoMode) { this.demoMode = demoMode; }
    public int getMinScore() { return minScore; }
    public void setMinScore(int minScore) { this.minScore = minScore; }
    public int getRequestDelayMs() { return requestDelayMs; }
    public void setRequestDelayMs(int requestDelayMs) { this.requestDelayMs = requestDelayMs; }
    public int getMaxProfilesPerRun() { return maxProfilesPerRun; }
    public void setMaxProfilesPerRun(int maxProfilesPerRun) { this.maxProfilesPerRun = maxProfilesPerRun; }
    public Yandex getYandex() { return yandex; }
    public Profi getProfi() { return profi; }
    public Hh getHh() { return hh; }
}