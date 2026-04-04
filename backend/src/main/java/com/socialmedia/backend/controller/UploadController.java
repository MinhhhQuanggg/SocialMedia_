package com.socialmedia.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        return handleUpload(file, "images", false);
    }

    @PostMapping("/post")
    public ResponseEntity<?> uploadPostImage(@RequestParam("file") MultipartFile file) {
        return handleUpload(file, "post", false);
    }

    @PostMapping("/media")
    public ResponseEntity<?> uploadMedia(@RequestParam("file") MultipartFile file) {
        return handleUpload(file, "media", true);
    }

    private ResponseEntity<?> handleUpload(MultipartFile file, String subfolder, boolean allowVideo) {
        System.out.println("[UPLOAD] file name: " + (file != null ? file.getOriginalFilename() : "null"));
        System.out.println("[UPLOAD] subfolder: " + subfolder);
        if (file == null || file.isEmpty()) {
            System.out.println("[UPLOAD] file is empty or null");
            return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded or file is empty"));
        }

        String contentType = file.getContentType();
        boolean isImage = contentType != null && contentType.startsWith("image/");
        boolean isVideo = contentType != null && contentType.startsWith("video/");
        if (!isImage && !(allowVideo && isVideo)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unsupported file type"));
        }
        
        try {
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString() + (ext != null ? "." + ext : "");
            
            // Create uploads/subfolder directory structure
            Path dirPath = Paths.get(System.getProperty("user.dir")).resolve("uploads").resolve(subfolder);
            System.out.println("[UPLOAD] upload dir: " + dirPath.toAbsolutePath());
            
            if (!Files.exists(dirPath)) {
                System.out.println("[UPLOAD] creating upload directory");
                Files.createDirectories(dirPath);
            }
            
            Path filePath = dirPath.resolve(filename);
            System.out.println("[UPLOAD] saving to: " + filePath.toAbsolutePath());
            Files.write(filePath, file.getBytes());
            
            String url = "/uploads/" + subfolder + "/" + filename;
            System.out.println("[UPLOAD] saved successfully");
            System.out.println("[UPLOAD] returning url: " + url);
            
            return ResponseEntity.ok().body(Map.of("url", url));
        } catch (IOException e) {
            System.err.println("[UPLOAD] Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }
}
