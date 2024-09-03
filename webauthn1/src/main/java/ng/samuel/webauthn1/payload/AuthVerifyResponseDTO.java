package ng.samuel.webauthn1.payload;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthVerifyResponseDTO {

    private String userName;

    private JsonNode key;

}
