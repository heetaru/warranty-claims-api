package com.stanislav.warrantyclaims.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, Long> {
    boolean existsByClaimId(Long claimId);

    List<ClaimDocument> findByClaimIdOrderByUploadedAtAsc(Long claimId);

    Optional<ClaimDocument> findByIdAndClaimId(Long id, Long claimId);
}

