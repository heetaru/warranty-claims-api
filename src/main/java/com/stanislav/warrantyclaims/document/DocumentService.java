package com.stanislav.warrantyclaims.document;

import com.stanislav.warrantyclaims.claim.Claim;
import com.stanislav.warrantyclaims.claim.ClaimService;
import com.stanislav.warrantyclaims.claim.ClaimStatus;
import com.stanislav.warrantyclaims.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/png", "image/jpeg");

    private final ClaimService claimService;
    private final ClaimDocumentRepository documentRepository;
    private final S3Storage storage;

    public DocumentResponse upload(Long claimId, MultipartFile file, Authentication user) {
        Claim claim = claimService.findAccessibleClaim(claimId, user);
        claimService.requireApplicantOwner(claim, user);
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "Documents can be added only to a draft claim");
        }
        if (file.isEmpty() || file.getSize() > MAX_SIZE || !ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload a PDF, PNG or JPEG file up to 5 MB");
        }

        try {
            byte[] bytes = file.getBytes();
            if (!hasMatchingSignature(bytes, file.getContentType())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "File content does not match its type");
            }
            String key = UUID.randomUUID().toString();
            storage.upload(key, bytes, file.getContentType());
            try {
                ClaimDocument document = new ClaimDocument(
                        claimId, key, safeFileName(file.getOriginalFilename()), file.getContentType(), bytes.length
                );
                return DocumentResponse.from(documentRepository.save(document));
            } catch (RuntimeException exception) {
                storage.deleteQuietly(key);
                throw exception;
            }
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
        }
    }

    public List<DocumentResponse> findAll(Long claimId, Authentication user) {
        claimService.findAccessibleClaim(claimId, user);
        return documentRepository.findByClaimIdOrderByUploadedAtAsc(claimId).stream()
                .map(DocumentResponse::from).toList();
    }

    public DownloadedDocument download(Long claimId, Long documentId, Authentication user) {
        claimService.findAccessibleClaim(claimId, user);
        ClaimDocument document = documentRepository.findByIdAndClaimId(documentId, claimId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found"));
        return new DownloadedDocument(document.getFileName(), document.getContentType(),
                storage.download(document.getObjectKey()));
    }

    private boolean hasMatchingSignature(byte[] bytes, String contentType) {
        if ("application/pdf".equals(contentType)) {
            return bytes.length >= 5 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D'
                    && bytes[3] == 'F' && bytes[4] == '-';
        }
        if ("image/png".equals(contentType)) {
            return bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 'P'
                    && bytes[2] == 'N' && bytes[3] == 'G' && bytes[4] == 13
                    && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10;
        }
        return bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF;
    }

    private String safeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "document";
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\r\\n]", "_");
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }
}

