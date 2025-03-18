package edu.diploma;

import edu.diploma.auth.JwtHelper;
import edu.diploma.auth.UserCreds;
import edu.diploma.controller.AuthController;
import edu.diploma.model.*;
import edu.diploma.repository.UserRepository;
import edu.diploma.service.AuthService;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

public class AuthControllerTest {

    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    public void setUp() {

    }

    @Test
    public void testPostLogin400WhenCreentialsNull() {

        UserCreds creds = null;

        final String msg = "Неправильные учетные данные";
        LoginErrors errors = new LoginErrors().addEmailMsg(msg);

        authService = mock(AuthService.class);
        when(authService.login(creds)).thenReturn(Either.left(errors));

        authController = new AuthController(authService);

        ResponseEntity<?> response = authController.postLogin(creds);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(((LoginErrors) response.getBody() ).getEmail().size(), is(1) );
        assertThat(
                ((LoginErrors) response.getBody()).getEmail().get(0),
                is(msg)
        );
    }

    @Test
    public void testPostLogin400WhenLoginBlank() {
        final String username = "   \t \n ";
        final String password = "123pwd";

        UserCreds creds = new UserCreds(username,password);

        final String msg = "Необходимо ввести почту";
        LoginErrors errors = new LoginErrors().addEmailMsg(msg);

        authService = mock(AuthService.class);
        when(authService.login(creds)).thenReturn(Either.left(errors));

        authController = new AuthController(authService);

        ResponseEntity<?> response = authController.postLogin(creds);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(((LoginErrors) response.getBody() ).getEmail().size(), is(1) );
        assertThat(
                ((LoginErrors) response.getBody()).getEmail().get(0),
                is(msg)
        );
    }

    @Test
    public void testPostLogin400WhenLoginNull() {
        final String username = null;
        final String password = "123pwd";
        UserCreds creds = new UserCreds(username,password);

        final String msg = "Необходимо ввести почту";
        LoginErrors errors = new LoginErrors().addEmailMsg(msg);

        authService = mock(AuthService.class);
        when(authService.login(creds)).thenReturn(Either.left(errors));

        authController = new AuthController(authService);

        ResponseEntity<?> response = authController.postLogin(creds);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(((LoginErrors) response.getBody() ).getEmail().size(), is(1) );
        assertThat(
                ((LoginErrors) response.getBody()).getEmail().get(0),
                is(msg)
        );
    }

    @Test
    public void testPostLogin400WhenPasswordNull() {
        final String username = "user1";
        final String password = null;
        UserCreds creds = new UserCreds(username,password);

        final String msg = "Необходимо ввести пароль";
        LoginErrors errors = new LoginErrors().addPasswordMsg(msg);

        authService = mock(AuthService.class);
        when(authService.login(creds)).thenReturn(Either.left(errors));

        authController = new AuthController(authService);

        ResponseEntity<?> response = authController.postLogin(creds);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(((LoginErrors) response.getBody() ).getPassword().size(), is(1) );
        assertThat(
                ((LoginErrors) response.getBody()).getPassword().get(0),
                is(msg)
        );
    }

    @Test
    public void testPostLogin400NoSuchLogin() {
        final String username = "user1";
        final String password = "123pwd";
        UserCreds creds = new UserCreds(username,password);

        final String msg = "Неправильно указана почта";
        LoginErrors errors = new LoginErrors().addEmailMsg(msg);

        authService = mock(AuthService.class);
        when(authService.login(creds)).thenReturn(Either.left(errors));

        authController = new AuthController(authService);

        ResponseEntity<?> response = authController.postLogin(creds);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(((LoginErrors) response.getBody() ).getEmail().size(), is(1) );
        assertThat(
                ((LoginErrors) response.getBody()).getEmail().get(0),
                is(msg)
        );
    }

    @Test
    public void testPostLogin400PasswordMismatch() {
        final String username = "user1";
        final String password = "123pwd";
        UserCreds creds = new UserCreds(username,password);

        final String msg = "Неправильно указан пароль";
        LoginErrors errors = new LoginErrors().addPasswordMsg(msg);

        authService = mock(AuthService.class);
        when(authService.login(creds)).thenReturn(Either.left(errors));

        authController = new AuthController(authService);

        ResponseEntity<?> response = authController.postLogin(creds);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(((LoginErrors) response.getBody() ).getPassword().size(), is(1) );
        assertThat(
                ((LoginErrors) response.getBody() ).getPassword().get(0),
                is(msg)
        );
    }

    @Test
    public void testPostLoginOk() {
        final String username = "user1";
        final String password = "123pwd";
        UserCreds creds = new UserCreds(username,password);

        final String token = "abcd1212x.zLKL.t789Bgre";

        final User user = new User(1L,username, password);

        Login login = new Login(token);
        authService = mock(AuthService.class);
        when(authService.login(creds)).thenReturn(Either.right(login));

        authController = new AuthController(authService);

        ResponseEntity<?> response = authController.postLogin(creds);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(((Login) response.getBody() ).getToken(), is(token) );
    }
}
