package ng.samuel.webauthn1.service;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import jakarta.transaction.Transactional;
import ng.samuel.webauthn1.entity.AuthSupport;
import ng.samuel.webauthn1.entity.AuthUser;
import ng.samuel.webauthn1.entity.Authenticator;
import ng.samuel.webauthn1.repository.AuthSupportRepository;
import ng.samuel.webauthn1.repository.AuthUserRepo;
import ng.samuel.webauthn1.repository.AuthenticatorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class RegistrationService implements CredentialRepository {
    @Autowired
    private AuthUserRepo authUserRepo;

    @Autowired
    private AuthSupportRepository authSupportRepository;

    @Autowired
    private AuthenticatorRepository authenticatorRepository;


    @Transactional
    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        AuthUser user = authUserRepo.findByUserName(username);
        List<Authenticator> auth = authenticatorRepository.findAllByUser(user);
        return auth.stream()
                .map(credential -> PublicKeyCredentialDescriptor.builder()
                        .id(credential.getCredentialId()).build()).collect(Collectors.toSet());
    }

    @Transactional
    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        AuthUser user = authUserRepo.findByUserName(username);

        return Optional.of(user.getHandle());
    }

    @Transactional
    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
       AuthUser user = authUserRepo.findByHandle(userHandle);

        return Optional.of(user.getUserName());
    }

    @Transactional
    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        AuthSupport authSupport = authSupportRepository.findByCredId(credentialId.getBase64Url());
        Optional<Authenticator> auth = authenticatorRepository.findByName(authSupport.getUserName());

        try {
            if(auth.isPresent()){
                Authenticator credential = auth.get();
                RegisteredCredential registeredCredential = RegisteredCredential.builder()
                        .credentialId(credential.getCredentialId())
                        .userHandle(credential.getUser().getHandle())
                        //.userHandle(credential.getCredentialId())
                        .publicKeyCose(credential.getPublicKey())
                        .signatureCount(credential.getCount())
                        .build();

                return Optional.of(registeredCredential);
            }
            else {
                return Optional.empty();
            }
        } catch (Exception e) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Transactional
    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        List<Authenticator> auth = authenticatorRepository.findAllByCredentialId(credentialId);
        return auth.stream()
                .map(credential -> RegisteredCredential.builder()
                        .credentialId(credential.getCredentialId())
                        .userHandle(credential.getUser().getHandle())
                        .publicKeyCose(credential.getPublicKey())
                        .signatureCount(credential.getCount())
                        .build())
                .collect(Collectors.toSet());

    }
}
