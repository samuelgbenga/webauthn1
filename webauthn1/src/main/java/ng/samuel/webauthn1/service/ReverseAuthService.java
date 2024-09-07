package ng.samuel.webauthn1.service;

import ng.samuel.webauthn1.payload.AuthRegisterResponse;
import ng.samuel.webauthn1.payload.AuthVerifyResponseDTO;

public interface ReverseAuthService {

    AuthRegisterResponse registerAuthUser(String userName);

    boolean finishRegisterAuthUser(String username, String credential);

    AuthVerifyResponseDTO startLogin(String userName);

    boolean finishLogin(String userName, String credential);



}
