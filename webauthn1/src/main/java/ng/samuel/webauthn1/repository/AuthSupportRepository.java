package ng.samuel.webauthn1.repository;

import ng.samuel.webauthn1.entity.AuthSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthSupportRepository extends JpaRepository<AuthSupport, String> {
    AuthSupport findByCredId(String base64Url);

    // Custom query to return only the list of IDs
    @Query("SELECT a.id FROM AuthSupport a")
    List<String> findAllIds();
}
