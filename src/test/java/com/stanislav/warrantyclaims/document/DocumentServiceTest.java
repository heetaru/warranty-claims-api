package com.stanislav.warrantyclaims.document;

import com.stanislav.warrantyclaims.claim.Claim;
import com.stanislav.warrantyclaims.claim.ClaimService;
import com.stanislav.warrantyclaims.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    @Mock ClaimService claimService;
    @Mock ClaimDocumentRepository documentRepository;
    @Mock S3Storage storage;
    @InjectMocks DocumentService documentService;

    private Authentication applicant;

    @BeforeEach
    void setUp() {
        applicant = UsernamePasswordAuthenticationToken.authenticated(
                "applicant", "password", List.of(new SimpleGrantedAuthority("ROLE_APPLICANT"))
        );
        when(claimService.findAccessibleClaim(1L, applicant))
                .thenReturn(new Claim("applicant", "POL-1001", "Broken laptop"));
    }

    @Test
    void rejectsFileWhoseContentDoesNotMatchItsType() {
        MockMultipartFile file = new MockMultipartFile("file", "proof.pdf", "application/pdf",
                "This is not a PDF".getBytes());

        ApiException error = assertThrows(ApiException.class,
                () -> documentService.upload(1L, file, applicant));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verify(storage, never()).upload(any(), any(), any());
    }

    @Test
    void storesValidPdfAndReturnsMetadata() {
        byte[] pdf = "%PDF-1.4\nexample".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "proof.pdf", "application/pdf", pdf);
        when(documentRepository.save(any())).thenAnswer(invocation -> {
            ClaimDocument document = invocation.getArgument(0);
            document.setId(4L);
            return document;
        });

        DocumentResponse response = documentService.upload(1L, file, applicant);

        assertEquals(4L, response.id());
        assertEquals("proof.pdf", response.fileName());
        verify(storage).upload(any(), eq(pdf), eq("application/pdf"));
    }
}

