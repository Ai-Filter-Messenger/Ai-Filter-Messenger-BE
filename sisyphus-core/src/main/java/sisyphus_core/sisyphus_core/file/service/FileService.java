package sisyphus_core.sisyphus_core.file.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.repository.UserRepository;
import sisyphus_core.sisyphus_core.chat.exception.ChatRoomNotFoundException;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.Message;
import sisyphus_core.sisyphus_core.chat.model.UserChatRoom;
import sisyphus_core.sisyphus_core.chat.model.dto.MessageType;
import sisyphus_core.sisyphus_core.chat.repository.ChatRoomRepository;
import sisyphus_core.sisyphus_core.chat.repository.UserChatRoomRepository;
import sisyphus_core.sisyphus_core.chat.service.KafkaProducerService;
import sisyphus_core.sisyphus_core.file.model.UploadFile;
import sisyphus_core.sisyphus_core.file.model.dto.UploadFileResponse;
import sisyphus_core.sisyphus_core.file.repository.FileRepository;

import java.io.File;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final AmazonS3Client amazonS3Client;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final SimpMessagingTemplate template;
    private final KafkaProducerService kafkaProducerService;
    private final ChatRoomRepository chatRoomRepository;
    private final UserChatRoomRepository userChatRoomRepository;

    @Value("${aws.cloud.s3.bucket}")
    private String bucket;

    @Value("${file.storage.path}")
    private String fileStoragePath;

    private Long sequence = 1L;

    //파일 업로드
    @Transactional
    public UploadFileResponse.SuccessResponse upload(List<MultipartFile> files, Long roomId, String loginId){
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));
        List<UserChatRoom> userChatRoomsByChatRoom = userChatRoomRepository.findUserChatRoomsByChatRoom(chatRoom);
        String fileUrl = "";
        StringBuilder fileUrls = new StringBuilder();
        boolean isNotContainReportedFile = true;
        for (MultipartFile file : files) {
            Map<String, Object> fileInfo = uploadFile(file);
            fileUrl = (String) fileInfo.get("fileUrl");
            Long fileSize = (Long) fileInfo.get("fileSize");

            //파일 검증 요청
            boolean isReported = false;
            if(!validFileRequest(fileUrl)){
                isReported = true;
                isNotContainReportedFile = false;
            }

            fileUrls.append(fileUrl).append(",");
            UploadFile uploadFile = UploadFile.builder()
                    .nickname(user.getNickname())
                    .fileUrl(fileUrl)
                    .fileSize(fileSize)
                    .chatRoomId(roomId)
                    .isReported(isReported)
                    .build();
            fileRepository.save(uploadFile);
        }

        if (fileUrls.length() > 0) {
            fileUrls.setLength(fileUrls.length() - 1);
        }

        Message message = Message.builder().message(fileUrls.toString()).roomId(roomId).type(MessageType.FILE).senderName(user.getNickname()).build();
        kafkaProducerService.sendMessage(message);
        template.convertAndSend("/topic/chatroom/" + message.getRoomId(), message);
        for (UserChatRoom userChatRoom : userChatRoomsByChatRoom) {
            template.convertAndSend("/queue/chatroom/" + userChatRoom.getUser().getNickname(), message);
        }

        return toFileState(fileUrl, isNotContainReportedFile);
    }

    //유효한 파일 검증
    public boolean validFileRequest(String fileUrl){
        RestTemplate restTemplate = new RestTemplate();
        String image_id = String.valueOf(sequence++);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = Map.of(
                "image_id", image_id,
                "image_url", fileUrl
        );

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try{
            ResponseEntity<Map> response = restTemplate.postForEntity("https://jygwagmi.shop/app/images",
                    requestEntity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            Map<String, Object> signUpResponse = (Map<String, Object>) responseBody.get("signUpResponse");
            boolean isSuccess = (Boolean) signUpResponse.get("isSuccess");

            if(!isSuccess) return false;
        }catch(Exception e){
            throw new RuntimeException("파일 검증 실패", e);
        }

        return true;
    }

    //파일리스폰스로 변환
    public UploadFileResponse.SuccessResponse toFileState (String fileUrl, boolean isNotContainReportedFile){
        return UploadFileResponse.SuccessResponse.builder()
                .fileUrl(fileUrl)
                .isSuccess(isNotContainReportedFile)
                .build();
    }

    //채팅방 이미지 변경
    @Transactional
    public String uploadAtChatRoom(MultipartFile file){
        Map<String, Object> fileInfo = uploadFile(file);
        return (String) fileInfo.get("fileUrl");
    }

    //유저의 파일 목록조회
    @Transactional
    public List<UploadFileResponse> findByUser(String loginId){
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        return toResponse(fileRepository.findByNickname(user.getNickname()));
    }

    //채팅방별 파일 목록조회
    @Transactional
    public List<UploadFileResponse> findByChatRoom(Long chatRoomId){
        List<UploadFile> byChatRoomId = fileRepository.findByChatRoomId(chatRoomId);
        return toResponse(byChatRoomId);
    }

    //s3 파일 업로드
    public Map<String, Object> uploadFile(MultipartFile file) {

        UUID uuid = UUID.randomUUID();
        String fileName = uuid + "_" + file.getOriginalFilename();

        File saveFile = new File(fileStoragePath, fileName);

        try {
            // 로컬에 파일 저장
            file.transferTo(saveFile);
        } catch (IOException e) {
            throw new RuntimeException("파일을 저장하는 동안 오류가 발생했습니다.");
        }

        // S3에 파일 업로드
        amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, saveFile)
                .withCannedAcl(CannedAccessControlList.PublicRead));

        // 업로드된 파일의 URL 가져오기
        String fileUrl = amazonS3Client.getUrl(bucket, fileName).toString();

        // S3에서 파일 메타데이터 가져오기
        ObjectMetadata metadata = amazonS3Client.getObjectMetadata(bucket, fileName);
        long fileSize = metadata.getContentLength(); // 파일 크기 (바이트 단위)

        // 로컬 파일 삭제
        saveFile.delete();

        // fileUrl과 fileSize를 Map으로 반환
        Map<String, Object> response = new HashMap<>();
        response.put("fileUrl", fileUrl);
        response.put("fileSize", fileSize);

        return response;
    }

    //파일 삭제
    @Transactional
    public void deleteFile(String fileUrl){
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/")+1);
        amazonS3Client.deleteObject(bucket, fileName);
        fileRepository.deleteByFileUrl(fileUrl);
    }

    public List<UploadFileResponse> toResponse(List<UploadFile> files){
        List<UploadFileResponse> toResponseFile = new ArrayList<>();

        for (UploadFile uploadFile : files) {
            UploadFileResponse uploadFileResponse = UploadFileResponse.builder()
                    .fileUrl(uploadFile.getFileUrl())
                    .createAt(uploadFile.getCreateAt())
                    .nickname(uploadFile.getNickname())
                    .fileSize(uploadFile.getFileSize())
                    .isReported(uploadFile.isReported())
                    .build();

            toResponseFile.add(uploadFileResponse);
        }

        return toResponseFile;
    }
}
