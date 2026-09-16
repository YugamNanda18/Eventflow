package com.eventflow.auth;

import com.eventflow.auth.dto.AuthResponse;
import com.eventflow.auth.dto.LoginRequest;
import com.eventflow.auth.dto.RegisterRequest;
import com.eventflow.common.exception.ApiException;
import com.eventflow.tenant.Organization;
import com.eventflow.tenant.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "User email already registered");
        }

        String orgSlug = request.getOrganizationName().toLowerCase().replaceAll("[^a-z0-9]", "-");
        Organization org = organizationRepository.findBySlug(orgSlug).orElseGet(() -> {
            Organization newOrg = new Organization("org_" + UUID.randomUUID().toString().replace("-", ""), request.getOrganizationName(), orgSlug);
            return organizationRepository.save(newOrg);
        });

        Role adminRole = roleRepository.findById("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "ADMIN", "Administrator")));

        User user = new User(
                "usr_" + UUID.randomUUID().toString().replace("-", ""),
                org,
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName()
        );
        user.setRoles(Set.of(adminRole));
        userRepository.save(user);

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getEmail(), org.getId(), roleNames);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }

        if (!user.isActive() || !user.getOrganization().isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "User or Organization is disabled");
        }

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getEmail(), user.getOrganization().getId(), roleNames);
    }
}
