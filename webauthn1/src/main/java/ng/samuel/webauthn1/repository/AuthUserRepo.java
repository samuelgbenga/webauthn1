package ng.samuel.webauthn1.repository;

import com.yubico.webauthn.data.ByteArray;
import ng.samuel.webauthn1.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthUserRepo extends JpaRepository<AuthUser, String> {
    AuthUser findByUserName(String username);

    AuthUser findByHandle(ByteArray userHandle);

    // Custom query to return only the list of IDs
    @Query("SELECT a.id FROM AuthUser a")
    List<String> findAllIds();

}
