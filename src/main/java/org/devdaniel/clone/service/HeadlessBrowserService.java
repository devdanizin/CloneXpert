package org.devdaniel.clone.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.apache.commons.io.FileUtils;
import org.devdaniel.clone.config.ClonerProperties;
import org.devdaniel.clone.util.ZipUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class HeadlessBrowserService {

    private final ClonerProperties props;
    private final SiteClonerService siteClonerService;

    public HeadlessBrowserService(ClonerProperties props, SiteClonerService siteClonerService) {
        this.props = props;
        this.siteClonerService = siteClonerService;
    }

    public ResponseEntity<?> cloneWithHeadless(String url, int maxDepth, boolean allowExternal) throws Exception {
        var startUri = java.net.URI.create(url);
        String host = startUri.getHost();
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        Path jobDir = Files.createTempDirectory(host + "_" + jobId);

        try (Playwright pw = Playwright.create()) {
            BrowserType.LaunchOptions opts = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(Arrays.asList(
                            "--no-sandbox",
                            "--disable-setuid-sandbox",
                            "--disable-dev-shm-usage",
                            "--disable-http2", // força HTTP/1.1
                            "--disable-background-timer-throttling",
                            "--disable-backgrounding-occluded-windows",
                            "--disable-renderer-backgrounding"
                    ));

            Browser browser = pw.chromium().launch(opts);

            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                    .setIgnoreHTTPSErrors(true);

            BrowserContext context = browser.newContext(contextOptions);
            Page page = context.newPage();
            page.setDefaultTimeout(45000); // 45 segundos

            ConcurrentLinkedQueue<Request> requests = new ConcurrentLinkedQueue<>();
            page.onRequest(requests::add);

            boolean loaded = false;
            int maxAttempts = 3;
            for (int attempt = 1; attempt <= maxAttempts && !loaded; attempt++) {
                try {
                    System.out.println("Navegando para " + url + ", tentativa " + attempt + "...");
                    page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                    loaded = true;
                } catch (PlaywrightException e) {
                    System.err.println("Erro na tentativa " + attempt + ": " + e.getMessage());
                    if (attempt == maxAttempts) {
                        throw e;
                    }
                    Thread.sleep(2000);
                }
            }

            String content = page.content();
            Path savedHtmlPath = siteClonerService.saveHtml(url, content, jobDir);

            for (Request req : requests) {
                try {
                    Response res = req.response();
                    if (res == null) continue;

                    String rUrl = req.url();
                    if (!rUrl.startsWith("http")) continue;

                    if (!allowExternal) {
                        java.net.URI ru = java.net.URI.create(rUrl);
                        if (!Objects.equals(ru.getHost(), host)) continue;
                    }

                    siteClonerService.downloadAsset(rUrl, jobDir);
                } catch (Exception ignored) {
                }
            }

            try {
                String localHtml = siteClonerService.rewriteReferencesToLocal(savedHtmlPath, jobDir);
                Files.writeString(savedHtmlPath, localHtml);
            } catch (Exception ex) {
                System.err.println("Erro reescrevendo HTML headless: " + ex.getMessage());
            }

            context.close();
            browser.close();
        }

        Path zipPath = Files.createTempFile(host + "_" + jobId, ".zip");
        ZipUtils.zipDirectory(jobDir, zipPath);
        FileUtils.deleteDirectory(jobDir.toFile());

        if (Files.notExists(zipPath)) {
            return ResponseEntity.status(500).body("Erro ao criar arquivo ZIP.");
        }

        InputStreamResource resource = new InputStreamResource(Files.newInputStream(zipPath));
        String filename = host + "_" + jobId + ".zip";

        ResponseEntity<?> response = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(Files.size(zipPath))
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(resource);

        Files.deleteIfExists(zipPath);
        return response;
    }
}