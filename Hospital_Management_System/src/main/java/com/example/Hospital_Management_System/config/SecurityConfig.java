package com.example.Hospital_Management_System.config;


import com.example.Hospital_Management_System.constant.USER_ROLE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {
  @Autowired
  private Filter filter;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http     .cors(Customizer.withDefaults())
                .csrf((csrf) ->
                        csrf.disable())
                .authorizeHttpRequests((auth) ->
                        auth.requestMatchers(
                                        "/user/doctor/make_as_schelduded/{appointmentId}",
                                        "/user/doctor/make_as_canceled/{appointmentId}",
                                        "/medicalrecord/save"
                                ).hasRole("DOCTOR") // or "ROLE_DOCTOR" depending on enum

                                .requestMatchers(
                                        "/admin/register",
                                        "/appointment/create",
                                        "/appointment/findall",
                                        "/department/save",
                                        "/user/patient/findall"
                                ).hasRole("ADMIN")

                                .requestMatchers(
                                        "/appointment/find-by-doctorid/{docId}",
                                        "/appointment/find-by-docid-and-status/**"
                                ).hasAnyRole("DOCTOR", "ADMIN")

                                .requestMatchers(
                                        "/appointment/findbypatientid/{patientId}",
                                        "/user/patient/{patiendId}"
                                ).hasAnyRole("ADMIN", "PATIENT")

                                .requestMatchers(
                                        "/docs",
                                        "/department/findall",
                                        "/department/{id}",
                                        "/user/hello",
                                        "/user/doctor/register",
                                        "/login",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/swagger-ui.html",
                                       "/user/patient_register"
                                         // Temporary wildcard for testing
                                ).permitAll()
                                .anyRequest().authenticated()
                )
                .sessionManagement((session) ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );
        // Removed httpBasic()

        http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
  @Autowired
  private UserDetailsService userDetailsService;
  @Bean
    public AuthenticationProvider authenticationProvider(){
      DaoAuthenticationProvider provider= new DaoAuthenticationProvider();
      provider.setPasswordEncoder(new BCryptPasswordEncoder(5));
      provider.setUserDetailsService(userDetailsService);
      return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception{
    return authenticationConfiguration.getAuthenticationManager();
  }
}
