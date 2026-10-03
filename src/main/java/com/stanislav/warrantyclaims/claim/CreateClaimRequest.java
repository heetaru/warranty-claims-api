package com.stanislav.warrantyclaims.claim;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateClaimRequest(
        @NotBlank @Size(max = 60) @Pattern(regexp = "[A-Za-z0-9-]+") String warrantyNumber,
        @NotBlank @Size(max = 1000) String description
) {
}
