package edu.diploma;

import edu.diploma.auth.JwtFilter;
import edu.diploma.auth.JwtHelper;
import edu.diploma.model.User;
import edu.diploma.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Example;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${edu.diploma.security.jwt.authorization-header:auth-token}")
    private String AUTHORIZATION_HEADER;

    @Value("${edu.diploma.security.permit:/login}")
    private String PERMIT;

    private final JwtFilter jwtFilter;
    private final JwtHelper jwtHelper;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Autowired
    public SecurityConfig(
            JwtFilter jwtFilter,
            JwtHelper jwtHelper,
            UserDetailsService userDetailsService,
            UserRepository userRepository
    ) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
        this.jwtHelper = jwtHelper;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity security) throws Exception {
        return security
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers(PERMIT).permitAll()
                                .anyRequest().authenticated()
                )
                .logout(logout -> logout.logoutSuccessHandler((request,response,auth) -> {
                    Optional.ofNullable(request.getHeader(AUTHORIZATION_HEADER)).map(
                            header -> header.split("\s")
                    ).map(
                            pieces -> pieces[pieces.length - 1]
                    ).map(
                            jwt -> jwtHelper.extractUsername(jwt)
                    ).flatMap(
                            username -> userRepository.findOne(Example.of(new User(username)))
                    ).map(user -> {
                        user.setToken(null);
                        return user;
                    }).ifPresent(user -> userRepository.save(user));
                }))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(
                username -> userDetailsService.loadUserByUsername(username)
        );
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
