package org.devdaniel.clone.controller;

import org.devdaniel.clone.entity.Profile;
import org.devdaniel.clone.scheduled.DeleteFiles;
import org.devdaniel.clone.security.CustomUserDetails;
import org.devdaniel.clone.service.ProfileService;
import org.devdaniel.clone.service.SiteClonerService;
import org.devdaniel.clone.service.HeadlessBrowserService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ClonerController {

    private final SiteClonerService clonerService;
    private final HeadlessBrowserService headlessBrowserService;
    private final DeleteFiles deleteFiles;
    private final ProfileService profileService;

    public ClonerController(SiteClonerService clonerService, HeadlessBrowserService headlessBrowserService, DeleteFiles deleteFiles, ProfileService profileService) {
        this.clonerService = clonerService;
        this.headlessBrowserService = headlessBrowserService;
        this.deleteFiles = deleteFiles;
        this.profileService = profileService;
    }

    @GetMapping("/clone")
    public ResponseEntity<?> cloneSite(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam String url,
            @RequestParam(defaultValue = "1") int depth,
            @RequestParam(defaultValue = "false") boolean allowExternal,
            @RequestParam(defaultValue = "static") String mode,
            @RequestParam(defaultValue = "10") int quantity) {

        boolean creditsRemoved = profileService.removeCredits(user.getId(), quantity);
        if (!creditsRemoved) {
            return ResponseEntity.status(403).body("Não foi possível remover créditos do perfil. Clonagem não iniciada.");
        }

        try {
            if ("headless".equalsIgnoreCase(mode)) {
                return headlessBrowserService.cloneWithHeadless(url, depth, allowExternal);
            } else {
                return clonerService.cloneSite(url, depth, allowExternal);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @GetMapping("/download/{jobId}")
    public ResponseEntity<InputStreamResource> downloadZip(@PathVariable String jobId) {
        try {
            Path zipPath = clonerService.getZipPath(jobId);

            if (zipPath == null || Files.notExists(zipPath)) {
                return ResponseEntity.notFound().build();
            }

            InputStreamResource resource = new InputStreamResource(Files.newInputStream(zipPath));
            String filename = zipPath.getFileName().toString();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentLength(Files.size(zipPath))
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .body(resource);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }


}