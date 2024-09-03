package ng.samuel.webauthn1.repository;

import ng.samuel.webauthn1.entity.AuthSupport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSupportRepository extends JpaRepository<AuthSupport, String> {
    AuthSupport findByCredId(String base64Url);
}
