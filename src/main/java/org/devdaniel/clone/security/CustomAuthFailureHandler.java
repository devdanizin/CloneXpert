package org.devdaniel.clone.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {

        String errorMessage = "Ocorreu um erro ao tentar fazer login.";

        if (exception instanceof org.springframework.security.authentication.BadCredentialsException) {
            errorMessage = "Nome de usuário ou senha incorretos.";
        } else if (exception instanceof org.springframework.security.core.userdetails.UsernameNotFoundException) {
            errorMessage = "Usuário não encontrado.";
        }

        request.getSession().setAttribute("loginError", errorMessage);
        response.sendRedirect("/login?error=true");

    }

}
