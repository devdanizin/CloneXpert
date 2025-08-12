package org.devdaniel.clone.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Nome de usuário ou senha incorretos.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Você saiu com sucesso.");
        }
        return "login";
    }
}