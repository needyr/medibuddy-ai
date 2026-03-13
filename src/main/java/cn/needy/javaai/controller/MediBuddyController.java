package cn.needy.javaai.controller;

import cn.needy.javaai.common.Result;
import cn.needy.javaai.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/medibuddy")
@RequiredArgsConstructor
public class MediBuddyController {

    private final FileUploadService fileUploadService;

    // 已禁用：AI 走 WebSocket，不再提供 HTTP chat 接口
//    @PostMapping("/chat")
//    public String chat(@RequestBody ChatForm chatForm) {
//        return mediBuddyAgent.chat(String.valueOf(chatForm.getMemoryId()), chatForm.getUserMessage());
//    }

    // 文件上传接口（HTTP 保留用于 CRUD/文件类功能）
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file")MultipartFile file) {
        return Result.success(fileUploadService.uploadFile(file));
    }
}
