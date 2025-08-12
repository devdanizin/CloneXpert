package org.devdaniel.clone.service;

import org.devdaniel.clone.config.ClonerProperties;
import org.devdaniel.clone.util.UrlUtils;
import org.apache.commons.io.FileUtils;
import org.devdaniel.clone.util.ZipUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SiteClonerService {

    private final ClonerProperties props;
    private final HttpClient httpClient;

    public SiteClonerService(ClonerProperties props) {
        this.props = props;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public ResponseEntity<?> cloneSite(String startUrl, int maxDepth, boolean allowExternal) throws Exception {
        URI startUri = URI.create(startUrl);
        String host = startUri.getHost();
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        Path jobDir = Path.of(props.getBaseDir(), host + "_" + jobId);
        Files.createDirectories(jobDir);

        ConcurrentLinkedQueue<UrlDepth> queue = new ConcurrentLinkedQueue<>();
        Set<String> visited = ConcurrentHashMap.newKeySet();

        queue.add(new UrlDepth(startUrl, 0));
        visited.add(startUrl);

        ExecutorService pageFetchers = Executors.newFixedThreadPool(Math.max(2, props.getMaxConcurrency()));
        ExecutorService assetFetchers = Executors.newFixedThreadPool(Math.max(2, props.getMaxConcurrency()));
        AtomicInteger activeTasks = new AtomicInteger(0);

        while (!queue.isEmpty()) {
            UrlDepth ud = queue.poll();
            if (ud == null) break;
            if (ud.depth > maxDepth) continue;

            activeTasks.incrementAndGet();
            pageFetchers.submit(() -> {
                try {
                    System.out.println("[JOB " + jobId + "] Fetching page: " + ud.url);
                    Document doc = fetchHtml(ud.url);
                    if (doc == null) return;

                    Path savedHtmlPath = saveHtml(ud.url, doc.html(), jobDir);

                    List<String> assets = new ArrayList<>();
                    Elements imgs = doc.select("img[src]");
                    for (Element e : imgs) assets.add(e.attr("abs:src"));

                    Elements scripts = doc.select("script[src]");
                    for (Element e : scripts) assets.add(e.attr("abs:src"));

                    Elements links = doc.select("link[href]");
                    for (Element e : links) {
                        String rel = e.attr("rel");
                        if (rel == null || rel.isEmpty() || rel.equalsIgnoreCase("stylesheet") || rel.toLowerCase().contains("icon")) {
                            assets.add(e.attr("abs:href"));
                        }
                    }

                    String htmlText = doc.html();
                    assets.addAll(UrlUtils.extractCssUrls(htmlText, ud.url));

                    for (String asset : assets.stream().filter(a -> a != null && !a.isBlank()).collect(Collectors.toSet())) {
                        if (!allowExternal) {
                            URI aUri = URI.create(asset);
                            if (!Objects.equals(aUri.getHost(), host)) continue;
                        }
                        assetFetchers.submit(() -> {
                            try {
                                downloadAsset(asset, jobDir);
                            } catch (Exception ex) {
                                System.err.println("Erro baixando asset " + asset + ": " + ex.getMessage());
                            }
                        });
                    }

                    Elements anchors = doc.select("a[href]");
                    for (Element a : anchors) {
                        String absHref = a.attr("abs:href");
                        if (absHref == null || absHref.isBlank()) continue;
                        URI hrefUri = URI.create(absHref);
                        String scheme = hrefUri.getScheme();
                        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) continue;

                        if (!allowExternal && !Objects.equals(hrefUri.getHost(), host)) continue;

                        String normalized = UrlUtils.normalizeUrl(absHref);
                        if (visited.add(normalized) && ud.depth + 1 <= maxDepth) {
                            queue.add(new UrlDepth(normalized, ud.depth + 1));
                        }
                    }

                    try {
                        String localHtml = rewriteReferencesToLocal(savedHtmlPath, jobDir);
                        Files.writeString(savedHtmlPath, localHtml);
                    } catch (Exception ex) {
                        System.err.println("Erro reescrevendo links locais: " + ex.getMessage());
                    }

                } catch (Exception e) {
                    System.err.println("Erro no fetch/page: " + e.getMessage());
                } finally {
                    activeTasks.decrementAndGet();
                }
            });

            if (queue.isEmpty()) {
                while (activeTasks.get() > 0) {
                    Thread.sleep(200);
                }
            }
        }

        pageFetchers.shutdown();
        assetFetchers.shutdown();
        pageFetchers.awaitTermination(5, TimeUnit.MINUTES);
        assetFetchers.awaitTermination(5, TimeUnit.MINUTES);

        Path index = jobDir.resolve("index.html");
        String indexContent = "<html><body><h1>Cloned site: " + startUrl + "</h1>" +
                "<p>Abra a página inicial salva em: <a href=\"" + UrlUtils.urlToLocalPath(startUrl, jobDir) + "\">início</a></p></body></html>";
        Files.writeString(index, indexContent);

        Path zipPath = Path.of(props.getBaseDir(), host + "_" + jobId + ".zip");
        ZipUtils.zipDirectory(jobDir, zipPath);

        FileUtils.deleteDirectory(jobDir.toFile());

        InputStreamResource resource = new InputStreamResource(Files.newInputStream(zipPath));
        String filename = zipPath.getFileName().toString();

        // Retornar o ResponseEntity com o arquivo zip para download direto
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(Files.size(zipPath))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    public Path getZipPath(String jobId) {
        File baseDir = new File(props.getBaseDir());
        File[] matches = baseDir.listFiles((dir, name) -> name.endsWith("_" + jobId + ".zip"));
        if (matches != null && matches.length > 0) {
            return matches[0].toPath();
        }
        return null;
    }

    private Document fetchHtml(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(props.getDefaultUserAgent())
                    .timeout(15000)
                    .followRedirects(true)
                    .get();
        } catch (IOException e) {
            System.err.println("Erro ao buscar HTML: " + e.getMessage());
            return null;
        }
    }

    public Path saveHtml(String pageUrl, String html, Path jobDir) throws IOException {
        String localPath = UrlUtils.urlToLocalPath(pageUrl, jobDir);
        Path out = Path.of(localPath);
        Files.createDirectories(out.getParent());
        Files.writeString(out, html);
        return out;
    }

    public void downloadAsset(String assetUrl, Path jobDir) throws Exception {
        if (assetUrl == null || assetUrl.isBlank()) return;
        URI uri = URI.create(assetUrl);
        String localPath = UrlUtils.urlToAssetPath(assetUrl, jobDir);
        Path out = Path.of(localPath);
        if (Files.exists(out)) return;
        Files.createDirectories(out.getParent());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("User-Agent", props.getDefaultUserAgent())
                .GET()
                .build();

        HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            Files.write(out, resp.body());
            System.out.println("Saved asset: " + assetUrl + " -> " + out);
        } else {
            System.err.println("Failed to download asset: " + assetUrl + " status=" + resp.statusCode());
        }
    }

    public String rewriteReferencesToLocal(Path savedHtmlPath, Path jobDir) throws IOException {
        String html = Files.readString(savedHtmlPath);
        Pattern p = Pattern.compile("(src|href)=[\"']([^\"'>]+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String attr = m.group(1);
            String url = m.group(2);
            String replacement = m.group(0);
            try {
                String abs = url;
                if (abs.startsWith("http://") || abs.startsWith("https://")) {
                    String local = UrlUtils.urlToAssetPath(abs, jobDir);
                    Path rel = jobDir.relativize(Path.of(local));
                    replacement = attr + "=\"" + rel.toString().replace('\\', '/') + "\"";
                }
            } catch (Exception ignored) {}
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);

        return UrlUtils.rewriteCssUrlsToLocal(sb.toString(), jobDir);
    }

    private record UrlDepth(String url, int depth) {}
}