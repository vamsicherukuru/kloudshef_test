package com.kloudshef.backend.service;

import com.kloudshef.backend.dto.request.LoginRequest;
import com.kloudshef.backend.dto.request.RegisterRequest;
import com.kloudshef.backend.dto.response.AuthResponse;
import com.kloudshef.backend.entity.Cook;
import com.kloudshef.backend.entity.Role;
import com.kloudshef.backend.entity.User;
import com.kloudshef.backend.exception.BadRequestException;
import com.kloudshef.backend.repository.CookRepository;
import com.kloudshef.backend.repository.UserRepository;
import com.kloudshef.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CookRepository cookRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        String firstName = request.getFirstName().trim();
        String lastName  = request.getLastName().trim();
        String fullName  = firstName + " " + lastName;

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .fullName(fullName)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .city(request.getCity())
                .role(request.getRole() != null ? request.getRole() : Role.CUSTOMER)
                .termsAcceptedAt(java.time.LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        Long cookId = null;
        if (user.getRole() == Role.COOK) {
            String kitchenName = (request.getKitchenName() != null && !request.getKitchenName().isBlank())
                    ? request.getKitchenName().trim()
                    : fullName + "'s Kitchen";
            String handle = generateHandle(request.getKitchenHandle(), kitchenName);
            Cook cook = Cook.builder()
                    .user(user)
                    .kitchenName(kitchenName)
                    .kitchenHandle(handle)
                    .city(user.getCity())
                    .dateOfBirth(request.getDateOfBirth())
                    .build();
            cook = cookRepository.save(cook);
            cookId = cook.getId();
        }

        return AuthResponse.builder()
                .token(jwtUtil.generateToken(user))
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .cookId(cookId)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        Long cookId = null;
        if (user.getRole() == Role.COOK) {
            cookId = cookRepository.findByUserId(user.getId()).map(Cook::getId).orElse(null);
        }

        return AuthResponse.builder()
                .token(jwtUtil.generateToken(user))
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .cookId(cookId)
                .build();
    }

    /**
     * Generates a unique kitchen handle.
     */
    private String generateHandle(String requested, String kitchenName) {
        if (requested != null && !requested.isBlank()) {
            String clean = requested.trim().toLowerCase()
                    .replaceAll("[^a-z0-9_]", "");
            if (!clean.isEmpty() && !cookRepository.existsByKitchenHandle(clean)) {
                return clean;
            }
        }
        String base = kitchenName.toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .trim()
                .replaceAll("\\s+", "_");
        if (base.isEmpty()) base = "kitchen";
        String candidate = base;
        int suffix = 1;
        while (cookRepository.existsByKitchenHandle(candidate)) {
            candidate = base + "_" + suffix;
            suffix++;
        }
        return candidate;
    }
}
