package edu.diploma.controller;

import edu.diploma.model.LoginErrors;
import edu.diploma.model.Login;
import edu.diploma.auth.UserCreds;
import edu.diploma.service.AuthService;
import io.vavr.control.Either;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService
    ) {
        this.authService = authService;
    }


    @PostMapping("/login")
    public ResponseEntity<?> postLogin(@RequestBody UserCreds creds) {

        Either<LoginErrors, Login> login = authService.login(creds);

        return login.isRight()
                ? ResponseEntity.ok().body(login.get())
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(login.getLeft());

    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<LoginErrors> handleException(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new LoginErrors().addEmailMsg("Неправильные учетные данные")
        );
    }
}