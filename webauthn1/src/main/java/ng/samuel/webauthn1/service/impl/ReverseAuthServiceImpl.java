package ng.samuel.webauthn1.service.impl;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import lombok.RequiredArgsConstructor;
import ng.samuel.webauthn1.entity.AuthSupport;
import ng.samuel.webauthn1.entity.AuthUser;
import ng.samuel.webauthn1.entity.Authenticator;
import ng.samuel.webauthn1.payload.AuthRegisterResponse;
import ng.samuel.webauthn1.payload.AuthVerifyResponseDTO;
import ng.samuel.webauthn1.repository.AuthSupportRepository;
import ng.samuel.webauthn1.repository.AuthUserRepo;
import ng.samuel.webauthn1.repository.AuthenticatorRepository;
import ng.samuel.webauthn1.service.ReverseAuthService;
import ng.samuel.webauthn1.utils.GenerateRandom;
import org.apache.catalina.User;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReverseAuthServiceImpl implements ReverseAuthService {
    private final RelyingParty relyingParty;

    private final CacheManager cacheManager;

    private final ObjectMapper mapper;

    private final AuthUserRepo authUserRepo;

    private final AuthenticatorRepository authenticatorRepository;

    private final AuthSupportRepository authSupportRepository;

    private final GenerateRandom generateRandom;


    @Override
    public AuthRegisterResponse registerAuthUser(String userName) {
       boolean existingUser = authUserRepo.existsById(userName);

       if(!existingUser){
           UserIdentity userIdentity = UserIdentity.builder()
                   .name(userName)
                   .displayName(userName)
                   .id(generateRandom.generateRandomId(32))
                   .build();

           AuthUser saveAuthUser = new AuthUser(userIdentity);
           try {
               authUserRepo.save(saveAuthUser);
               return newAuthRegistration(saveAuthUser);
              // return AuthRegisterResponse.builder().userName(saveAuthUser.getUserName()).build();
           }catch (Exception e){
               throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save user.", e);
           }
       }else {
           throw new ResponseStatusException(HttpStatus.CONFLICT, "Username "+ userName + " already exists. Choose a new name");
       }

    }



    private AuthRegisterResponse newAuthRegistration(AuthUser saveAuthUser) {
        UserIdentity userIdentity = saveAuthUser.toUserIdentity();

        StartRegistrationOptions registrationOptions = StartRegistrationOptions.builder().user(userIdentity).build();

        PublicKeyCredentialCreationOptions registration = relyingParty.startRegistration(registrationOptions);

       addPkcToCache(saveAuthUser.getUserName(), registration);

        try {
            return AuthRegisterResponse.builder()
                    .userName(saveAuthUser.getUserName())
                    .key(mapper.readTree(registration.toCredentialsCreateJson()))
                    .build();
        }catch (JsonProcessingException e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing JSON.", e);
        }

    }

    private void addPkcToCache(String userName, PublicKeyCredentialCreationOptions registration) {
        Objects.requireNonNull(cacheManager.getCache("pkc")).put(userName, registration);
    }

    @Override
    public boolean finishRegisterAuthUser(String username, String credential) {
       try{
           AuthUser authUser = authUserRepo.findById(username).orElseThrow(
                   ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

           PublicKeyCredentialCreationOptions requestOptions =  getPkcFromCache(username);

           System.out.println(credential);

           if (requestOptions != null){
               PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc = PublicKeyCredential.parseRegistrationResponseJson(credential);
               FinishRegistrationOptions options = FinishRegistrationOptions.builder()
                       .request(requestOptions)
                       .response(pkc)
                       .build();

               System.out.println(options);

               RegistrationResult result = relyingParty.finishRegistration(options);
               Authenticator savedAuth = new Authenticator(result, pkc.getResponse(), authUser, username);
               authenticatorRepository.save(savedAuth);
               AuthSupport authSupport = new AuthSupport();
               authSupport.setUserName(username);
               authSupport.setCredId(result.getKeyId().getId().getBase64Url());
               authSupportRepository.save(authSupport);
               return true;

           }
           else {
               throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cached request expired. Try to register again");
           }
       }
    catch (RegistrationFailedException | IOException e) {
           throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Registration Failed HA HA.", e);
       }
    }


//    @Override
//    public boolean finishRegisterAuthUser(String username, String credential) {
//        try{
//            AuthUser authUser = authUserRepo.findById(username).orElseThrow(
//                    ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
//
//            PublicKeyCredentialCreationOptions requestOptions =  getPkcFromCache(username);
//
//            if (requestOptions != null){
//                PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc = PublicKeyCredential.parseRegistrationResponseJson(credential);
//                FinishRegistrationOptions options = FinishRegistrationOptions.builder()
//                        .request(requestOptions)
//                        .response(pkc)
//                        .build();
//
//                RegistrationResult result = relyingParty.finishRegistration(options);
//                Authenticator savedAuth = new Authenticator(result, pkc.getResponse(), authUser, username);
//                authenticatorRepository.save(savedAuth);
//                AuthSupport authSupport = new AuthSupport();
//                authSupport.setUserName(username);
//                authSupport.setCredId(result.getKeyId().getId().getBase64Url());
//                authSupportRepository.save(authSupport);
//                return true;
//
//            }
////            else {
////                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cached request expired. Try to register again");
////            }
//        }
//        catch (Exception e) {
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Registration Failed HA HA.", e);
//        }
//
//        return true;
//    }

    private PublicKeyCredentialCreationOptions getPkcFromCache(String username) {
        Object cachedValue = cacheManager.getCache("pkc").get(username).get();
        if (cachedValue instanceof PublicKeyCredentialCreationOptions) {
            return (PublicKeyCredentialCreationOptions) cachedValue;
        } else {
            throw new IllegalStateException("Cached value is not of type PublicKeyCredentialCreationOptions");
        }
    }


    @Override
    public AuthVerifyResponseDTO startLogin(String userName) {
        AssertionRequest request = relyingParty.startAssertion(StartAssertionOptions.builder().username(userName).build());

        try{
            addRequestToCache(userName, request);
            return AuthVerifyResponseDTO.builder()
                    .userName(userName)
                    .key(mapper.readTree(request.toCredentialsGetJson()))
                    .build();
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
    private void addRequestToCache(String userName, AssertionRequest request) {
        Objects.requireNonNull(cacheManager.getCache("pkc-verify")).put(userName, request);
    }



    @Override
    public boolean finishLogin(String userName, String credential) {
        try{
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
                    PublicKeyCredential.parseAssertionResponseJson(credential);
            AssertionRequest request =  getRequestFromCache(userName);

            FinishAssertionOptions finishAssertionOptions = FinishAssertionOptions.builder()
                    .request(request)
                    .response(pkc)
                    .build();

            System.out.println(finishAssertionOptions);

            AssertionResult result = relyingParty.finishAssertion(
                    finishAssertionOptions
            );

            return result.isSuccess();

        } catch (IOException | AssertionFailedException e) {
            throw new RuntimeException("Authentication failed",e);
        }
    }






    private AssertionRequest getRequestFromCache(String userName) {
    // Retrieve the object from the cache using the provided username
    Object cachedObject = Objects.requireNonNull(cacheManager.getCache("pkc-verify"))
            .get(userName)
            .get();

    // Ensure the object is of type AssertionRequest
    if (cachedObject instanceof AssertionRequest) {
        return (AssertionRequest) cachedObject;
    } else {
        throw new IllegalStateException("Cached object is not of type AssertionRequest");
    }
}

}
