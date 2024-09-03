package ng.samuel.webauthn1.entity;

import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.UserIdentity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.*;

import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Entity
public class AuthUser {

    @Id
    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_created_ts")
    private Long userCreatedTimeStamp;

    @Lob
    @Column(nullable = true, length = 64)
    private ByteArray handle;



    public AuthUser(UserIdentity userIdentity){
        this.handle = userIdentity.getId();
        this.userName = userIdentity.getName();
        this.userCreatedTimeStamp = Instant.now().getEpochSecond();
    }

    public UserIdentity toUserIdentity(){
        return UserIdentity.builder()
                .name(getUserName())
                .displayName(getUserName())
                .id(getHandle())
                .build();
    }




}
