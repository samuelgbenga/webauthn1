package ng.samuel.webauthn1.entity;


import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.data.AttestedCredentialData;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ByteArray;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Entity
@Getter
@NoArgsConstructor
public class Authenticator {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private String name;

    @Lob
    @Column(nullable = false)
    private ByteArray credentialId;

    @Lob
    @Column(nullable = false)
    private ByteArray publicKey;

    @Column(nullable = false)
    private Long count;

    @Lob
    @Column(nullable = true)
    private ByteArray aaguid;

    @ManyToOne
    private AuthUser user;

    public Authenticator(RegistrationResult result, AuthenticatorAttestationResponse response, AuthUser user, String name){

        Optional<AttestedCredentialData> attestationData = response.getAttestation().getAuthenticatorData().getAttestedCredentialData();

        this.credentialId = result.getKeyId().getId();

        this.publicKey = result.getKeyId().getId();

        this.aaguid = attestationData.map(AttestedCredentialData::getAaguid).orElse(null);

        this.count = result.getSignatureCount();

        this.name = name;

        this.user = user;

    }


}
