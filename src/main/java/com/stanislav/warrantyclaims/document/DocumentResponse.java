package com.stanislav.warrantyclaims.document;

import java.time.Instant;

public record DocumentResponse(Long id, String fileName, String contentType, long sizeBytes, Instant uploadedAt) {
    public static DocumentResponse from(ClaimDocument document) {
        return new DocumentResponse(document.getId(), document.getFileName(), document.getContentType(),
                document.getSizeBytes(), document.getUploadedAt());
    }
}

