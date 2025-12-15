package vn.project.magic_english.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        if (cloudName != null && !cloudName.isEmpty()
                && apiKey != null && !apiKey.isEmpty()
                && apiSecret != null && !apiSecret.isEmpty()) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true));
            System.out.println(">>> Cloudinary initialized successfully");
        } else {
            System.out.println(">>> Cloudinary not configured, using local storage fallback");
        }
    }

    public boolean isConfigured() {
        return cloudinary != null;
    }

    /**
     * Upload file to Cloudinary
     * 
     * @param file   MultipartFile to upload
     * @param folder Folder name in Cloudinary (e.g., "avatars", "achievements")
     * @return URL of uploaded file
     */
    public String upload(MultipartFile file, String folder) throws IOException {
        if (cloudinary == null) {
            throw new IllegalStateException("Cloudinary is not configured");
        }

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "auto"));

        return (String) uploadResult.get("secure_url");
    }

    /**
     * Upload file with custom public ID
     * 
     * @param file     MultipartFile to upload
     * @param folder   Folder name
     * @param publicId Custom public ID (filename without extension)
     * @return URL of uploaded file
     */
    public String upload(MultipartFile file, String folder, String publicId) throws IOException {
        if (cloudinary == null) {
            throw new IllegalStateException("Cloudinary is not configured");
        }

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", folder,
                "public_id", publicId,
                "resource_type", "auto",
                "overwrite", true));

        return (String) uploadResult.get("secure_url");
    }

    /**
     * Delete file from Cloudinary
     * 
     * @param publicId Public ID of the file to delete
     */
    public void delete(String publicId) throws IOException {
        if (cloudinary == null) {
            throw new IllegalStateException("Cloudinary is not configured");
        }

        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    /**
     * Extract public ID from Cloudinary URL
     * 
     * @param url Cloudinary URL
     * @return Public ID
     */
    public String getPublicIdFromUrl(String url) {
        if (url == null || !url.contains("cloudinary")) {
            return null;
        }
        // URL format:
        // https://res.cloudinary.com/{cloud_name}/image/upload/v{version}/{public_id}.{format}
        String[] parts = url.split("/upload/");
        if (parts.length > 1) {
            String path = parts[1];
            // Remove version if present
            if (path.startsWith("v")) {
                path = path.substring(path.indexOf("/") + 1);
            }
            // Remove file extension
            int dotIndex = path.lastIndexOf(".");
            if (dotIndex > 0) {
                path = path.substring(0, dotIndex);
            }
            return path;
        }
        return null;
    }
}
