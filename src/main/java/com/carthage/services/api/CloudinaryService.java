package com.carthage.services.api;

import com.carthage.config.Config;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CloudinaryService {

    private Cloudinary cloudinary;

    public CloudinaryService() {
        String cloudName = Config.get("CLOUDINARY_CLOUD_NAME");
        String apiKey = Config.get("CLOUDINARY_API_KEY");
        String apiSecret = Config.get("CLOUDINARY_API_SECRET");

        if (cloudName != null && apiKey != null && apiSecret != null) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true
            ));
        } else {
            System.err.println("Cloudinary credentials not found in environment.");
        }
    }

    public String uploadImage(File file) {
        if (cloudinary == null) {
            System.err.println("Cloudinary is not initialized.");
            return null;
        }
        try {
            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap("format", "jpg"));
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            System.err.println("Error uploading image to Cloudinary: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
