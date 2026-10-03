package com.stanislav.warrantyclaims.claim;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionRequest(
        @NotNull ClaimStatus decision,
        @Size(max = 500) String reason
) {
}

