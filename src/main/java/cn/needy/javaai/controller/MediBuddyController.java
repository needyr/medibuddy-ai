package cn.needy.javaai.controller;

import cn.needy.javaai.assistant.MediBuddyAgent;
import cn.needy.javaai.bean.ChatForm;
import cn.needy.javaai.common.Result;
import cn.needy.javaai.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/medibuddy")
@RequiredArgsConstructor
public class MediBuddyController {

    private final MediBuddyAgent mediBuddyAgent;
    private final FileUploadService fileUploadService;

    @PostMapping("/chat")
    public String chat(@RequestBody ChatForm chatForm) {
        return mediBuddyAgent.chat(String.valueOf(chatForm.getMemoryId()), chatForm.getUserMessage());
    }

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file")MultipartFile file) {
        return Result.success(fileUploadService.uploadFile(file));
    }
}