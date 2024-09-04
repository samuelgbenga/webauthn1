package ng.samuel.webauthn1.utils;


import com.yubico.webauthn.data.ByteArray;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Base64;

@Converter(autoApply = true)
public class ByteArrayAttributeConverter implements AttributeConverter<ByteArray, String> {

    // convert from bytearray to base64 that would be store in the
    // data base
    @Override
    public String convertToDatabaseColumn(ByteArray byteArray) {
        return byteArray == null ? null : byteArray.getBase64();
    }

    // convert from base64 to byte array back
    // this how you you collect it from the database
    @Override
    public ByteArray convertToEntityAttribute(String s) {
        return s == null ? null : new ByteArray(Base64.getDecoder().decode(s));
    }


//    public static void main(String[] args) {
//        ByteArray s = new ByteArray(Base64.getDecoder().decode("MM/Vc1zvbPg61qBCUv6s35h7nzIU0fU287jf0EbToGY="));
//
//        System.out.println(s);
//    }
}
