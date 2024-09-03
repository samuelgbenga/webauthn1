package ng.samuel.webauthn1.utils;


import com.yubico.webauthn.data.ByteArray;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class GenerateRandom {

    private static final SecureRandom random = new SecureRandom();

    public ByteArray generateRandomId(int length){
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return new ByteArray(bytes);
    }
}
