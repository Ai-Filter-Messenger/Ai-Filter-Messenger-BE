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
import java.util.concurrent.atomic.AtomicLong;

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

    // AtomicLong을 사용하여 고유 ID 생성
    private final AtomicLong idGenerator = new AtomicLong(1);

    //파일 업로드
    @Transactional
    public UploadFileResponse.SuccessResponse upload(List<MultipartFile> files, Long roomId, String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));
        List<UserChatRoom> userChatRoomsByChatRoom = userChatRoomRepository.findUserChatRoomsByChatRoom(chatRoom);

        StringBuilder validFileUrls = new StringBuilder();
        boolean isNotContainReportedFile = true;

        for (MultipartFile file : files) {
            Map<String, Object> fileInfo = uploadFile(file);
            String fileUrl = (String) fileInfo.get("fileUrl");
            Long fileSize = (Long) fileInfo.get("fileSize");

            // image_id 생성 (고유 정수)
            int imageId = generateImageId();

            // 파일 검증 요청
            boolean isReported = !validFileRequest(fileUrl, imageId);
            if (isReported) {
                isNotContainReportedFile = false;
                log.warn("불법 파일로 검출됨: {}", fileUrl);
            }

            // 모든 파일 저장 (불법 여부와 관계없이)
            UploadFile uploadFile = UploadFile.builder()
                    .nickname(user.getNickname())
                    .fileUrl(fileUrl)
                    .fileSize(fileSize)
                    .chatRoomId(roomId)
                    .isReported(isReported)
                    .build();
            fileRepository.save(uploadFile);

            // 유효한 파일만 사용자에게 반환할 URL에 포함
            if (!isReported) {
                validFileUrls.append(fileUrl).append(",");
            }
        }

        // 메시지 전송 및 반환 URL 조정
        if (validFileUrls.length() > 0) {
            validFileUrls.setLength(validFileUrls.length() - 1); // 마지막 쉼표 제거
            Message message = Message.builder()
                    .message(validFileUrls.toString())
                    .roomId(roomId)
                    .type(MessageType.FILE)
                    .senderName(user.getNickname())
                    .build();

            kafkaProducerService.sendMessage(message);
            template.convertAndSend("/topic/chatroom/" + message.getRoomId(), message);
            for (UserChatRoom userChatRoom : userChatRoomsByChatRoom) {
                template.convertAndSend("/queue/chatroom/" + userChatRoom.getUser().getNickname(), message);
            }
        }

        return toFileState(validFileUrls.toString(), isNotContainReportedFile);
    }


    // 고유 image_id 생성 메서드
    private int generateImageId() {
        return (int) idGenerator.getAndIncrement();
    }

    // 유효한 파일 검증
    public boolean validFileRequest(String fileUrl, int imageId) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "image_url", fileUrl,
                "image_id", imageId
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://jygwagmi.shop/app/images",
                    requestEntity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("AI 서버 응답 실패: 상태 코드 = {}", response.getStatusCode());
                throw new RuntimeException("AI 서버와의 통신 실패");
            }

            Map<String, Object> responseBody = response.getBody();
            log.debug("AI 서버 응답: {}", responseBody);

            if (responseBody == null || !responseBody.containsKey("result")) {
                log.error("AI 서버 응답 본문이 비어 있거나 'result' 필드가 없습니다. 응답 데이터: {}", responseBody);
                throw new RuntimeException("파일 검증 응답이 올바르지 않습니다.");
            }

            String resultValue = responseBody.get("result").toString();
            log.debug("파일 검증 결과: {}", resultValue);

            return !"fake_nsfw".equals(resultValue);
        } catch (Exception e) {
            log.error("파일 검증 중 예외 발생: ", e);
            throw new RuntimeException("파일 검증 실패", e);
        }
    }

    //파일리스폰스로 변환
    public UploadFileResponse.SuccessResponse toFileState(String fileUrl, boolean isNotContainReportedFile) {
        return UploadFileResponse.SuccessResponse.builder()
                .fileUrl(fileUrl) // 정상 파일만 포함
                .isSuccess(isNotContainReportedFile)
                .message(isNotContainReportedFile ? "파일 업로드에 성공했습니다" : "불법 파일이 포함되었습니다")
                .build();
    }


    // 모든 유저의 파일 조회
    @Transactional(readOnly = true)
    public List<UploadFileResponse> findAll() {
        List<UploadFile> files = fileRepository.findAll(); // 모든 파일 가져오기
        return toResponse(files); // DTO 변환
    }


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
