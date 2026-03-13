package cn.needy.medibuddy.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface FileUploadService {
    String uploadFile(MultipartFile file);
}
