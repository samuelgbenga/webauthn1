package ng.samuel.webauthn1.payload;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ng.samuel.webauthn1.entity.AuthUser;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthRegisterResponse {

    private String userName;

    private JsonNode key;



}
