package edu.diploma.controller;

import edu.diploma.model.LoginErrors;
import edu.diploma.model.Login;
import edu.diploma.auth.UserCreds;
import edu.diploma.service.AuthService;
import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(
            AuthService authService
    ) {
        this.authService = authService;
    }


    @PostMapping("/login")
    public ResponseEntity<?> postLogin(@RequestBody UserCreds creds) {

        Either<LoginErrors, Login> login = authService.login(creds);

        if (login.isRight()) {
            log.info(String.format("User logged in: %s", creds.getLogin()));
            return ResponseEntity.ok().body(login.get());
        }

        log.info(String.format("Login attempt failed: %s",login.getLeft()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(login.getLeft());

    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<LoginErrors> handleException(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new LoginErrors().addEmailMsg("Неправильные учетные данные")
        );
    }
}
