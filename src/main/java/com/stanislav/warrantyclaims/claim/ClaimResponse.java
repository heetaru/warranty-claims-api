package com.stanislav.warrantyclaims.claim;

import java.time.Instant;

public record ClaimResponse(
        Long id,
        String warrantyNumber,
        String description,
        ClaimStatus status,
        String decisionReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(
                claim.getId(), claim.getWarrantyNumber(), claim.getDescription(),
                claim.getStatus(), claim.getDecisionReason(), claim.getCreatedAt(), claim.getUpdatedAt()
        );
    }
}

