package com.stanislav.warrantyclaims.document;

import com.stanislav.warrantyclaims.common.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Slf4j
public class S3Storage {
    private final S3Client client;
    private final String bucket;

    public S3Storage(S3Client client, @Value("${app.s3.bucket}") String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    public void upload(String key, byte[] bytes, String contentType) {
        try {
            client.putObject(PutObjectRequest.builder()
                            .bucket(bucket).key(key).contentType(contentType).build(),
                    RequestBody.fromBytes(bytes));
        } catch (SdkException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Document storage is unavailable");
        }
    }

    public byte[] download(String key) {
        try {
            return client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
        } catch (SdkException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Document storage is unavailable");
        }
    }

    public void deleteQuietly(String key) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException exception) {
            log.warn("Could not remove orphaned object {}", key);
        }
    }

}
