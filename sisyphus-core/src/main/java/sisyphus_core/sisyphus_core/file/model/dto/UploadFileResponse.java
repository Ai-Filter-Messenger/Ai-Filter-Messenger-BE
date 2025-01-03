package sisyphus_core.sisyphus_core.file.model.dto;

import lombok.*;

import java.time.ZonedDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UploadFileResponse {
    private String nickname;
    private String fileUrl;
    private ZonedDateTime createAt;
    private boolean isReported;
    private Long fileSize;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @Setter
    public static class SuccessResponse {
        private String fileUrl;
        private boolean isSuccess;
        private String message;
    }
}
