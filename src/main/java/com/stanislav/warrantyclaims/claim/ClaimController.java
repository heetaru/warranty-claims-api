package com.stanislav.warrantyclaims.claim;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {
    private final ClaimService claimService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimResponse create(@Valid @RequestBody CreateClaimRequest request, Authentication user) {
        return claimService.create(request, user);
    }

    @GetMapping
    public List<ClaimResponse> findAll(Authentication user) {
        return claimService.findAll(user);
    }

    @GetMapping("/{id}")
    public ClaimResponse findById(@PathVariable Long id, Authentication user) {
        return claimService.findById(id, user);
    }

    @PostMapping("/{id}/submit")
    public ClaimResponse submit(@PathVariable Long id, Authentication user) {
        return claimService.submit(id, user);
    }

    @PostMapping("/{id}/decision")
    public ClaimResponse decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest request,
                                Authentication user) {
        return claimService.decide(id, request, user);
    }
}

