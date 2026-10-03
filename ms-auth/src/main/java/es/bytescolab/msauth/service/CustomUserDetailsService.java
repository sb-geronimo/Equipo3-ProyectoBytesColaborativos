package es.bytescolab.msauth.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.UUID;

public interface CustomUserDetailsService extends UserDetailsService {

    UserDetails loadUserByUserID(UUID userID) throws UsernameNotFoundException;
}
