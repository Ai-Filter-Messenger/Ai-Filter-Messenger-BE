package sisyphus_core.sisyphus_core.auth.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sisyphus_core.sisyphus_core.auth.model.dto.UserRole;
import sisyphus_core.sisyphus_core.auth.model.dto.UserState;
import sisyphus_core.sisyphus_core.chat.model.UserChatRoom;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(uniqueConstraints = {@UniqueConstraint(name = "uniqueloginId", columnNames = {"loginId", "nickname"})},name = "users")
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @NotNull
    private String loginId;

    @NotNull
    private String password;

    @NotNull
    private String nickname;

    private String name;
    private String describe;
    private String profileImageUrl;

    @Enumerated(value = EnumType.STRING)
    private UserState state;

    @Enumerated(value = EnumType.STRING)
    private UserRole userRole;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserChatRoom> chatRooms = new ArrayList<>();

    @Builder.Default
    private ZonedDateTime createAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
}
