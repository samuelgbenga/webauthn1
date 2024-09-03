package ng.samuel.webauthn1.repository;

import com.yubico.webauthn.data.ByteArray;
import ng.samuel.webauthn1.entity.AuthUser;
import ng.samuel.webauthn1.entity.Authenticator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthenticatorRepository extends JpaRepository<Authenticator, Long> {
    List<Authenticator> findAllByUser(AuthUser user);

    Optional<Authenticator> findByName(String userName);

    List<Authenticator> findAllByCredentialId(ByteArray credentialId);
}
