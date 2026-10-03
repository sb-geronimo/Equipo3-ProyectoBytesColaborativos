package es.bytescolab.msauth.service.impl;

import es.bytescolab.msauth.repository.UserRepository;
import es.bytescolab.msauth.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements CustomUserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    @Override
    public UserDetails loadUserByUserID(UUID userID) throws UsernameNotFoundException {
        return userRepository.findById(userID)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

}
