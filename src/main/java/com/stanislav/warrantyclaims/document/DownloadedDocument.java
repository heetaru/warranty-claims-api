package com.stanislav.warrantyclaims.document;

public record DownloadedDocument(String fileName, String contentType, byte[] bytes) {
}

