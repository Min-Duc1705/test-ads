package vn.project.magic_english.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    @Value("${magic_english.upload}")
    private String baseURI;

    @Autowired
    private CloudinaryService cloudinaryService;

    public void createDirectory(String folder) throws URISyntaxException {
        URI uri = new URI(folder);
        Path path = Paths.get(uri);
        File tmpDir = new File(path.toString());
        if (!tmpDir.isDirectory()) {
            try {
                Files.createDirectory(tmpDir.toPath());
                System.out.println(">>> CREATE NEW DIRECTORY SUCCESSFUL, PATH = " + tmpDir.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(">>> SKIP MAKING DIRECTORY, ALREADY EXISTS");
        }
    }

    /**
     * Store file - uses Cloudinary if configured, otherwise falls back to local
     * storage
     * 
     * @param file   MultipartFile to store
     * @param folder Folder name
     * @return URL (if Cloudinary) or filename (if local)
     */
    public String store(MultipartFile file, String folder) throws URISyntaxException, IOException {
        // Try Cloudinary first
        if (cloudinaryService.isConfigured()) {
            try {
                String url = cloudinaryService.upload(file, folder);
                System.out.println(">>> File uploaded to Cloudinary: " + url);
                return url;
            } catch (Exception e) {
                System.out.println(">>> Cloudinary upload failed, falling back to local: " + e.getMessage());
            }
        }

        // Fallback to local storage
        String finalName = System.currentTimeMillis() + "-" + file.getOriginalFilename();
        URI uri = new URI(baseURI + folder + "/" + finalName);
        Path path = Paths.get(uri);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
        }
        return finalName;
    }

    /**
     * Check if a stored path is a Cloudinary URL
     */
    public boolean isCloudinaryUrl(String path) {
        return path != null && path.contains("cloudinary");
    }

    public long getFileLength(String fileName, String folder) throws URISyntaxException {
        // Cloudinary URLs don't need length check
        if (isCloudinaryUrl(fileName)) {
            return -1; // Indicate it's a remote file
        }

        URI uri = new URI(baseURI + folder + "/" + fileName);
        Path path = Paths.get(uri);

        File tmpDir = new File(path.toString());

        // file không tồn tại, hoặc file là 1 director => return 0
        if (!tmpDir.exists() || tmpDir.isDirectory())
            return 0;
        return tmpDir.length();
    }

    public InputStreamResource getResource(String fileName, String folder)
            throws URISyntaxException, FileNotFoundException {
        // Cloudinary URLs should be accessed directly, not through this method
        if (isCloudinaryUrl(fileName)) {
            throw new UnsupportedOperationException("Use the Cloudinary URL directly for remote files");
        }

        URI uri = new URI(baseURI + folder + "/" + fileName);
        Path path = Paths.get(uri);

        File file = new File(path.toString());
        return new InputStreamResource(new FileInputStream(file));
    }
}
