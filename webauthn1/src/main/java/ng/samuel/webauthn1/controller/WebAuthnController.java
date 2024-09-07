package ng.samuel.webauthn1.controller;

import lombok.RequiredArgsConstructor;
import ng.samuel.webauthn1.payload.AuthRegisterResponse;
import ng.samuel.webauthn1.payload.AuthVerifyResponseDTO;

import ng.samuel.webauthn1.service.ReverseAuthService;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class WebAuthnController {
    private final ReverseAuthService reverseAuthService;

    @PostMapping("/register/start")
    public ResponseEntity<?> startRegistration(@RequestParam String username){
        AuthRegisterResponse response = reverseAuthService.registerAuthUser(username);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/finish")
    public ResponseEntity<Boolean> finishRegistration(@RequestParam String username, @RequestBody String credential){
        boolean success = reverseAuthService.finishRegisterAuthUser(username, credential);
        return ResponseEntity.ok(success);
    }


    @PostMapping("/login/start")
    public ResponseEntity<?> startLogin(@RequestParam String username){
        AuthVerifyResponseDTO response = reverseAuthService.startLogin(username);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/finish")
    public ResponseEntity<Boolean> finishLogin(@RequestParam String username, @RequestBody String credential){
        boolean success = reverseAuthService.finishLogin(username, credential);
        return ResponseEntity.ok(success);
    }


    @GetMapping("/testing")
    public ResponseEntity<String> testing()
    {
        return ResponseEntity.ok("It is secured");
    }
}
