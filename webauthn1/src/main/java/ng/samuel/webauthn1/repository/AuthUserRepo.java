package ng.samuel.webauthn1.repository;

import com.yubico.webauthn.data.ByteArray;
import ng.samuel.webauthn1.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthUserRepo extends JpaRepository<AuthUser, String> {
    AuthUser findByUserName(String username);

    AuthUser findByHandle(ByteArray userHandle);

}
