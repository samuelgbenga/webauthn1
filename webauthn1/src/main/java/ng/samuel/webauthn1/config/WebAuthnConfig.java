package ng.samuel.webauthn1.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "authn")
public class WebAuthnConfig {

    private String hostname;
    private String display;
    private String origin;
}
