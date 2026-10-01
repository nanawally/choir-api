package service

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream
import java.net.URI
import java.nio.file.Files

class StorageService(
    endpoint: String,
    accessKey: String,
    secretKey: String,
    region: String,
    private val bucket: String,
) {
    private val s3: S3Client = S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
        )
        .region(Region.of(region))
        .forcePathStyle(true)
        .httpClient(UrlConnectionHttpClient.builder().build())
        .build()

    fun upload(key: String, data: ByteArray, contentType: String) {
        s3.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(data),
        )
    }

    fun download(key: String): InputStream {
        return s3.getObject(
            GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build(),
        )
    }

    fun delete(key: String) {
        s3.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build(),
        )
    }

    companion object {
        /**
         * Compress a PDF using Ghostscript. Returns the compressed bytes,
         * or the original bytes if Ghostscript is not available or compression fails.
         */
        fun compressPdf(input: ByteArray): ByteArray {
            val inputFile = Files.createTempFile("gs-input-", ".pdf")
            val outputFile = Files.createTempFile("gs-output-", ".pdf")
            try {
                Files.write(inputFile, input)
                val process = ProcessBuilder(
                    "gs",
                    "-sDEVICE=pdfwrite",
                    "-dCompatibilityLevel=1.4",
                    "-dPDFSETTINGS=/ebook",
                    "-dNOPAUSE",
                    "-dBATCH",
                    "-sOutputFile=${outputFile.toAbsolutePath()}",
                    inputFile.toAbsolutePath().toString(),
                )
                    .redirectErrorStream(true)
                    .start()

                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    return input
                }

                val compressed = Files.readAllBytes(outputFile)
                // Only use compressed version if it's actually smaller
                return if (compressed.size < input.size) compressed else input
            } catch (_: Exception) {
                // Ghostscript not installed or other error — return original
                return input
            } finally {
                Files.deleteIfExists(inputFile)
                Files.deleteIfExists(outputFile)
            }
        }
    }
}
