package org.devdaniel.clone.controller;

import org.devdaniel.clone.entity.Profile;
import org.devdaniel.clone.repository.ProfileRepository;
import org.devdaniel.clone.security.CustomUserDetails;
import org.devdaniel.clone.service.FeedService;
import org.devdaniel.clone.service.HeadlessBrowserService;
import org.devdaniel.clone.service.ProfileService;
import org.devdaniel.clone.service.SiteClonerService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Controller
public class ClonerPageController {

    private final SiteClonerService clonerService;
    private final HeadlessBrowserService headlessBrowserService;
    private final ProfileRepository profileRepository;
    private final ProfileService profileService;
    private final FeedService feedService;

    public ClonerPageController(SiteClonerService clonerService, HeadlessBrowserService headlessBrowserService, ProfileRepository profileRepository, ProfileService profileService, FeedService feedService) {
        this.clonerService = clonerService;
        this.headlessBrowserService = headlessBrowserService;
        this.profileRepository = profileRepository;
        this.profileService = profileService;
        this.feedService = feedService;
    }

    @GetMapping({"/clonar"})
    public String showForm(@RequestParam(required = false) String url,
                           @RequestParam(defaultValue = "1") int depth,
                           @RequestParam(defaultValue = "false") boolean allowExternal,
                           @RequestParam(defaultValue = "static") String mode,
                           Model model) {
        model.addAttribute("url", url != null ? url : "");
        model.addAttribute("depth", depth);
        model.addAttribute("allowExternal", allowExternal);
        model.addAttribute("mode", mode);
        model.addAttribute("jobId", null);
        model.addAttribute("error", null);
        return "clone";
    }

    @PostMapping("/clone")
    public ResponseEntity<?> cloneSite(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       @RequestParam String url,
                                       @RequestParam(defaultValue = "1") int depth,
                                       @RequestParam(defaultValue = "false") boolean allowExternal,
                                       @RequestParam(defaultValue = "static") String mode) {
        try {
            ResponseEntity<?> cloneResponse;
            int quantity;
            if ("headless".equalsIgnoreCase(mode)) {
                cloneResponse = headlessBrowserService.cloneWithHeadless(url, depth, allowExternal);
                quantity = 20;
            } else {
                quantity = 10;
                cloneResponse = clonerService.cloneSite(url, depth, allowExternal);
            }

            if (cloneResponse.getStatusCode().is2xxSuccessful()) {
                boolean creditsRemoved = profileService.removeCredits(userDetails.getId(), quantity);
                if (!creditsRemoved) {
                    return ResponseEntity.status(500).body("Clonagem realizada, mas falha ao remover créditos.");
                }
            } else {
                return cloneResponse;
            }
            return cloneResponse;
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(302).header("Location", "/panel").build();
        }
    }

    @GetMapping("/")
    public String index() {
        return "home";
    }

    @GetMapping("/panel")
    public String painel(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Optional<Profile> user = profileRepository.findByName(userDetails.getUsername());
        model.addAttribute("usuario", user.orElse(null));
        return "painel";
    }

    @GetMapping("/admin")
    public String adminPage(Model model) {
        model.addAttribute("feedbacks", feedService.getAllFeeds());
        return "admin";
    }

    @GetMapping("/terms")
    public String termsPage(Model model) {
        return "terms";
    }

    @GetMapping("/dicas")
    public String dicasPage(Model model) {
        return "dicas";
    }

    @GetMapping("/feedback")
    public String feedbackPage(Model model) {
        return "feedback";
    }

    @GetMapping("/teste")
    public String testPage(Model model) {
        return "teste";
    }

}