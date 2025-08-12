package org.devdaniel.clone.util;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {

    private static final Pattern CSS_URL_PATTERN = Pattern.compile("url\\((?:'|\")?(.*?)(?:'|\")?\\)", Pattern.CASE_INSENSITIVE);

    public static List<String> extractCssUrls(String htmlOrCss, String baseUrl) {
        List<String> results = new ArrayList<>();
        Matcher m = CSS_URL_PATTERN.matcher(htmlOrCss);
        while (m.find()) {
            String url = m.group(1);
            if (url == null || url.isBlank()) continue;
            // try to resolve relative urls by naive join
            try {
                URI base = URI.create(baseUrl);
                URI resolved = base.resolve(url);
                results.add(resolved.toString());
            } catch (Exception e) {
                // fallback raw
                results.add(url);
            }
        }
        return results;
    }

    public static String normalizeUrl(String url) {
        try {
            URI u = URI.create(url);
            URI normalized = new URI(u.getScheme(), u.getAuthority(), u.getPath(), null, null);
            return normalized.toString();
        } catch (Exception e) {
            return url;
        }
    }

    public static String safeFileName(String input) {
        String s = input.replaceAll("[\\/:*?\"<>|]", "_");
        if (s.length() > 200) s = s.substring(0, 200);
        return s;
    }

    public static String urlToLocalPath(String pageUrl, Path jobDir) {
        try {
            URI u = URI.create(pageUrl);
            String path = u.getPath();
            if (path == null || path.isBlank() || path.equals("/")) path = "/index.html";
            if (!path.endsWith(".html") && !path.endsWith(".htm")) {
                if (path.endsWith("/")) path = path + "index.html";
                else path = path + "/index.html";
            }
            String host = u.getHost();
            String out = jobDir.resolve(host + path).toString();
            return out;
        } catch (Exception e) {
            // fallback
            String file = safeFileName(pageUrl) + ".html";
            return jobDir.resolve(file).toString();
        }
    }

    public static String urlToAssetPath(String assetUrl, Path jobDir) {
        try {
            URI u = URI.create(assetUrl);
            String host = u.getHost();
            String path = u.getPath();
            if (path == null || path.isBlank()) path = "/";
            String out = jobDir.resolve(host + path).toString();
            // if path ends with /, create index file
            if (out.endsWith(File.separator)) {
                out = out + "index";
            }
            // ensure extension
            String ext = FilenameUtils.getExtension(out);
            if (ext == null || ext.isBlank()) {
                out = out + ".dat";
            }
            return out;
        } catch (Exception e) {
            String file = safeFileName(assetUrl);
            return jobDir.resolve("assets").resolve(file).toString();
        }
    }

    public static String rewriteCssUrlsToLocal(String html, Path jobDir) {
        Matcher m = CSS_URL_PATTERN.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String url = m.group(1);
            String replacement = m.group(0);
            try {
                String local = urlToAssetPath(url, jobDir);
                Path rel = jobDir.relativize(Path.of(local));
                replacement = "url('" + rel.toString().replace('\\', '/') + "')";
            } catch (Exception e) {
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}