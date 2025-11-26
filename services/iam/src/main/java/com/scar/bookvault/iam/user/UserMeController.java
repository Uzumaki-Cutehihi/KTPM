package com.scar.bookvault.iam.user;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/iam/v1/users")
public class UserMeController {
    private final UserRepository repository;

    public UserMeController(UserRepository repository) { this.repository = repository; }

    @GetMapping("/me")
    public User me(@RequestParam String username) {
        return repository.findByUsername(username).orElseThrow();
    }
}

