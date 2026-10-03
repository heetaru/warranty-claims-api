package com.stanislav.warrantyclaims.claim;

import com.stanislav.warrantyclaims.common.ApiException;
import com.stanislav.warrantyclaims.document.ClaimDocumentRepository;
import com.stanislav.warrantyclaims.soap.WarrantyCheck;
import com.stanislav.warrantyclaims.soap.WarrantyGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {
    @Mock ClaimRepository claimRepository;
    @Mock ClaimDocumentRepository documentRepository;
    @Mock WarrantyGateway warrantyGateway;
    @InjectMocks ClaimService claimService;

    private Claim claim;
    private Authentication applicant;
    private Authentication reviewer;

    @BeforeEach
    void setUp() {
        claim = new Claim("applicant", "POL-1001", "Broken laptop screen");
        claim.setId(1L);
        applicant = user("applicant", "ROLE_APPLICANT");
        reviewer = user("reviewer", "ROLE_REVIEWER");
    }

    @Test
    void submitRequiresAtLeastOneDocument() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        ApiException error = assertThrows(ApiException.class, () -> claimService.submit(1L, applicant));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals(ClaimStatus.DRAFT, claim.getStatus());
        verify(warrantyGateway, never()).check(any());
    }

    @Test
    void expiredWarrantyRejectsTheClaimWithReason() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(documentRepository.existsByClaimId(1L)).thenReturn(true);
        when(warrantyGateway.check("POL-1001")).thenReturn(new WarrantyCheck(false, "Warranty expired"));
        when(claimRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClaimResponse response = claimService.submit(1L, applicant);

        assertEquals(ClaimStatus.REJECTED, response.status());
        assertEquals("Warranty expired", response.decisionReason());
    }

    @Test
    void reviewerCanApproveSubmittedClaim() {
        claim.setStatus(ClaimStatus.SUBMITTED);
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClaimResponse response = claimService.decide(1L, new DecisionRequest(ClaimStatus.APPROVED, null), reviewer);

        assertEquals(ClaimStatus.APPROVED, response.status());
        verify(claimRepository).save(claim);
    }

    @Test
    void anotherApplicantCannotReadClaim() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        ApiException error = assertThrows(ApiException.class,
                () -> claimService.findById(1L, user("someone-else", "ROLE_APPLICANT")));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }

    private Authentication user(String username, String role) {
        return UsernamePasswordAuthenticationToken.authenticated(
                username, "password", List.of(new SimpleGrantedAuthority(role))
        );
    }
}

