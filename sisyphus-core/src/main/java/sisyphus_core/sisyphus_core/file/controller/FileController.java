package sisyphus_core.sisyphus_core.file.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sisyphus_core.sisyphus_core.file.model.dto.UploadFileResponse;
import sisyphus_core.sisyphus_core.file.service.FileService;

import java.util.List;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<UploadFileResponse.SuccessResponse> upload(@RequestPart("files")List<MultipartFile> files,
                                         @RequestParam("roomId") Long roomId,
                                         Authentication auth){
        return ResponseEntity.ok().body(fileService.upload(files, roomId, auth.getName()));
    }

    @GetMapping("/user/find")
    public ResponseEntity<List<UploadFileResponse>> findByUser(Authentication auth){
        return ResponseEntity.ok().body(fileService.findByUser(auth.getName()));
    }

    @GetMapping("/chatroom/find")
    public ResponseEntity<List<UploadFileResponse>> findByChatRoom(@RequestParam("chatRoomId") Long chatRoomId){
        return ResponseEntity.ok().body(fileService.findByChatRoom(chatRoomId));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> delete(@RequestParam("fileUrl") String fileUrl){
        fileService.deleteFile(fileUrl);
        return ResponseEntity.ok("파일이 삭제되었습니다");
    }

    @GetMapping("/all")
    public ResponseEntity<List<UploadFileResponse>> findAllFiles() {
        return ResponseEntity.ok().body(fileService.findAll());
    }
}
