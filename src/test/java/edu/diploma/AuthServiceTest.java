package edu.diploma;

import edu.diploma.auth.JwtHelper;
import edu.diploma.auth.UserCreds;
import edu.diploma.model.Login;
import edu.diploma.model.LoginErrors;
import edu.diploma.model.User;
import edu.diploma.repository.UserRepository;
import edu.diploma.service.AuthService;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthServiceTest {

    private JwtHelper jwtHelper;
    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    public void setUp() {

    }

    @Test
    public void testPostLogin400WhenCreentialsNull() {

        UserCreds creds = null;

        authService = new AuthService(jwtHelper,userRepository,passwordEncoder);

        Either<LoginErrors,Login> result = authService.login(creds);

        assertThat(result.isLeft(), is(true));
        assertThat(result.getLeft().getEmail().size(), is(1) );
        assertThat(
                result.getLeft().getEmail().get(0),
                is("Неправильные учетные данные")
        );
    }

    @Test
    public void testLoginErrorWhenLoginBlank() {
        final String username = "   \t \n ";
        final String password = "123pwd";

        UserCreds creds = new UserCreds(username,password);

        authService = new AuthService(jwtHelper,userRepository,passwordEncoder);

        Either<LoginErrors,Login> result = authService.login(creds);

        assertThat(result.isLeft(), is(true));
        assertThat(result.getLeft().getEmail().size(), is(1) );
        assertThat(
                result.getLeft().getEmail().get(0),
                is("Необходимо ввести почту")
        );
    }

    @Test
    public void testLoginErrorWhenLoginNull() {
        final String username = null;
        final String password = "123pwd";
        UserCreds creds = new UserCreds(username,password);

        authService = new AuthService(jwtHelper,userRepository,passwordEncoder);

        Either<LoginErrors,Login> result = authService.login(creds);

        assertThat(result.isLeft(), is(true));
        assertThat(result.getLeft().getEmail().size(), is(1) );
        assertThat(
                result.getLeft().getEmail().get(0),
                is("Необходимо ввести почту")
        );
    }

    @Test
    public void testLoginErrorWhenPasswordBlank() {
        final String username = "user1";
        final String password = " \t \n ";
        UserCreds creds = new UserCreds(username,password);

        authService = new AuthService(jwtHelper,userRepository,passwordEncoder);

        Either<LoginErrors,Login> response = authService.login(creds);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getPassword().size(), is(1) );
        assertThat(
                response.getLeft().getPassword().get(0),
                is("Необходимо ввести пароль")
        );
    }

    @Test
    public void testLoginExceptionWhenPasswordNull() {
        final String username = "user1";
        final String password = null;
        UserCreds creds = new UserCreds(username,password);

        authService = new AuthService(jwtHelper,userRepository,passwordEncoder);

        Either<LoginErrors,Login> result = authService.login(creds);

        assertThat(result.isLeft(), is(true));
        assertThat(result.getLeft().getPassword().size(), is(1) );
        assertThat(
                result.getLeft().getPassword().get(0),
                is("Необходимо ввести пароль")
        );
    }

    @Test
    public void testLoginErrorNoSuchLogin() {
        final String username = "user1";
        final String password = "123pwd";
        UserCreds creds = new UserCreds(username,password);

        userRepository = mock(UserRepository.class);
        when(userRepository.findOne(any(Example.class))).thenReturn(Optional.empty());

        authService = new AuthService(jwtHelper,userRepository,passwordEncoder);

        Either<LoginErrors,Login> result = authService.login(creds);

        assertThat(result.isLeft(), is(true));
        assertThat(result.getLeft().getEmail().size(), is(1) );
        assertThat(
                result.getLeft().getEmail().get(0),
                is("Неправильно указана почта")
        );
    }

    @Test
    public void testLoginErrorPasswordMismatch() {
        final String username = "user1";
        final String password = "123pwd";
        UserCreds creds = new UserCreds(username,password);

        final String token = "abcd1212x.zLKL.t789Bgre";

        final User user = new User(1L,username, password);

        passwordEncoder = mock(BCryptPasswordEncoder.class);
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(false);

        userRepository = mock(UserRepository.class);
        when(userRepository.findOne(any(Example.class))).thenReturn(Optional.of(user));

        authService = new AuthService(jwtHelper,userRepository,passwordEncoder);

        Either<LoginErrors,Login> result = authService.login(creds);

        assertThat(result.isLeft(), is(true));
        assertThat(result.getLeft().getPassword().size(), is(1) );
        assertThat(
                result.getLeft().getPassword().get(0),
                is("Неправильно указан пароль")
        );
    }

    @Test
    public void testLoginOk() {
        final String username = "user1";
        final String password = "123pwd";
        UserCreds creds = new UserCreds(username,password);

        final String token = "abcd1212x.zLKL.t789Bgre";

        final User user = new User(1L,username, password);

        jwtHelper = mock(JwtHelper.class);
        when(jwtHelper.createToken(Map.ofEntries(),username)).thenReturn(token);

        passwordEncoder = mock(BCryptPasswordEncoder.class);
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

        userRepository = mock(UserRepository.class);
        when(userRepository.findOne(any(Example.class))).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(new User(username,password,token));

        authService = new AuthService(jwtHelper,userRepository,passwordEncoder);

        Either<LoginErrors,Login> result = authService.login(creds);

        assertThat(result.isRight(), is(true));
        assertThat(result.get().getToken(), is(token) );
    }
}
