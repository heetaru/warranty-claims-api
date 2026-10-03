package com.stanislav.warrantyclaims.claim;

import com.stanislav.warrantyclaims.common.ApiException;
import com.stanislav.warrantyclaims.document.ClaimDocumentRepository;
import com.stanislav.warrantyclaims.soap.WarrantyCheck;
import com.stanislav.warrantyclaims.soap.WarrantyGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClaimService {
    private final ClaimRepository claimRepository;
    private final ClaimDocumentRepository documentRepository;
    private final WarrantyGateway warrantyGateway;

    @Transactional
    public ClaimResponse create(CreateClaimRequest request, Authentication user) {
        Claim claim = new Claim(user.getName(), request.warrantyNumber(), request.description());
        return ClaimResponse.from(claimRepository.save(claim));
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> findAll(Authentication user) {
        List<Claim> claims = isReviewer(user)
                ? claimRepository.findAll()
                : claimRepository.findByOwnerUsernameOrderByCreatedAtDesc(user.getName());
        return claims.stream().map(ClaimResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ClaimResponse findById(Long id, Authentication user) {
        return ClaimResponse.from(findAccessibleClaim(id, user));
    }

    @Transactional
    public ClaimResponse submit(Long id, Authentication user) {
        Claim claim = findAccessibleClaim(id, user);
        requireApplicantOwner(claim, user);
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "Only a draft claim can be submitted");
        }
        if (!documentRepository.existsByClaimId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Upload a document before submitting the claim");
        }

        WarrantyCheck result = warrantyGateway.check(claim.getWarrantyNumber());
        claim.setStatus(result.active() ? ClaimStatus.SUBMITTED : ClaimStatus.REJECTED);
        claim.setDecisionReason(result.active() ? null : result.reason());
        claim.setUpdatedAt(Instant.now());
        return ClaimResponse.from(claimRepository.save(claim));
    }

    @Transactional
    public ClaimResponse decide(Long id, DecisionRequest request, Authentication user) {
        if (!isReviewer(user)) {
            throw new AccessDeniedException("Reviewer role is required");
        }
        Claim claim = findAccessibleClaim(id, user);
        if (claim.getStatus() != ClaimStatus.SUBMITTED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only a submitted claim can be reviewed");
        }
        if (request.decision() != ClaimStatus.APPROVED && request.decision() != ClaimStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Decision must be APPROVED or REJECTED");
        }
        if (request.decision() == ClaimStatus.REJECTED &&
                (request.reason() == null || request.reason().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A rejection reason is required");
        }
        claim.setStatus(request.decision());
        claim.setDecisionReason(request.decision() == ClaimStatus.REJECTED ? request.reason().trim() : null);
        claim.setUpdatedAt(Instant.now());
        return ClaimResponse.from(claimRepository.save(claim));
    }

    public Claim findAccessibleClaim(Long id, Authentication user) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Claim not found"));
        if (!isReviewer(user) && !claim.getOwnerUsername().equals(user.getName())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Claim not found");
        }
        return claim;
    }

    public void requireApplicantOwner(Claim claim, Authentication user) {
        if (isReviewer(user) || !claim.getOwnerUsername().equals(user.getName())) {
            throw new AccessDeniedException("Applicant role and ownership are required");
        }
    }

    private boolean isReviewer(Authentication user) {
        return user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_REVIEWER".equals(authority.getAuthority()));
    }
}

