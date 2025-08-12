package org.devdaniel.clone.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "cloner")
public class ClonerProperties {
    private String baseDir = "./cloned_sites";
    private int maxConcurrency = 8;
    private String defaultUserAgent = "SiteClonerBot/1.0 (+https://example.com)";

}