package edu.diploma.service;

import edu.diploma.auth.JwtHelper;
import edu.diploma.auth.UserCreds;
import edu.diploma.controller.AuthController;
import edu.diploma.model.Login;
import edu.diploma.model.LoginErrors;
import edu.diploma.model.User;
import edu.diploma.repository.UserRepository;
import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final JwtHelper jwtHelper;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(
            JwtHelper jwtHelper,
            UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.jwtHelper = jwtHelper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @PostMapping("/login")
    public Either<LoginErrors,Login> login(@RequestBody UserCreds creds) {

        LoginErrors errors = new LoginErrors();

        if (Objects.isNull(creds)) {
            log.info(String.format("Invalid credentials: null"));
            return Either.left(errors.addEmailMsg("Неправильные учетные данные"));
        }

        if (Objects.isNull(creds.getLogin()) || creds.getLogin().trim().isEmpty()) {
            log.info(String.format("User Email not provided"));
            return Either.left(errors.addEmailMsg("Необходимо ввести почту"));
        }

        if (Objects.isNull(creds.getPassword()) || creds.getPassword().trim().isEmpty()) {
            log.info(String.format("User Password not provided"));
            return Either.left(errors.addPasswordMsg("Необходимо ввести пароль"));
        }

        Optional<User> user = userRepository.findOne(Example.of(
                new User(creds.getLogin()),
                ExampleMatcher.matching()
                        .withMatcher("username", m -> m.exact())
                        .withIgnorePaths("password")
        ));

        if (user.isPresent()) {
            if (passwordEncoder.matches(creds.getPassword(), user.get().getPassword())) {
                user.get().setToken(jwtHelper.createToken(Map.ofEntries(), user.get().getUsername()));
                userRepository.save(user.get());
                log.info(String.format("User authenticated: %s", creds.getLogin()));
                return Either.right(new Login(user.get().getToken()));
            }

            log.info(String.format("Incorrect password"));
            return Either.left(errors.addPasswordMsg("Неправильно указан пароль"));
        }

        log.info(String.format("Incorrect email"));
        return Either.left(errors.addEmailMsg("Неправильно указана почта"));

    }
}
