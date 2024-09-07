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
import java.util.*;

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

        refreshAuthUserDb();

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

           //System.out.println(credential);

           if (requestOptions != null){
               PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc = PublicKeyCredential.parseRegistrationResponseJson(credential);
               FinishRegistrationOptions options = FinishRegistrationOptions.builder()
                       .request(requestOptions)
                       .response(pkc)
                       .build();

               //System.out.println(options);
               System.out.println("*****************************");
               System.out.println(requestOptions);
               System.out.println("*****************************");
               System.out.println(pkc);
               System.out.println("*****************************");
               System.out.println(pkc.getId().getBase64());
               System.out.println("*****************************");
               System.out.println(pkc.getId().getBase64Url());

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

        // step 1: start assertion
        AssertionRequest request = relyingParty.startAssertion(StartAssertionOptions.builder().username(userName).build());

       // step 2: check the db if the user exist
        AuthUser user = authUserRepo.findById(userName).orElseThrow(
                    ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // try and catch it
        try{
            // step 3: cache the user and the assertion request
            addRequestToCache(userName, request);

            // Step 4: return the necessary information the Authenticator
            // including: username(string), key(jsonNode). exclude handle.
            return AuthVerifyResponseDTO.builder()
                    .userName(userName)
                    .key(mapper.readTree(request.toCredentialsGetJson()))
                    .handle(user.getHandle())
                    .build();

            // catch the error
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

    }


    // the caches the request to match it with the response from the authenticator
    private void addRequestToCache(String userName, AssertionRequest request) {
        Objects.requireNonNull(cacheManager.getCache("pkc-verify")).put(userName, request);
    }



    // gets the two information one from the server(request) and the other from the authenticator(response)
    // and does the comparison using finishAssertion method (which did work so i had to do it manually)
    @Override
    public boolean finishLogin(String userName, String credential) {
        try{

            // step 1:  get the credential from the authenticator
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
                    PublicKeyCredential.parseAssertionResponseJson(credential);
            // step 2: get the previously cashed information
            AssertionRequest request =  getRequestFromCache(userName);

            // ( i did this so i will not include it as part of the step)
            AuthUser user = authUserRepo.findByUserName(userName);

            if (request.getPublicKeyCredentialRequestOptions().getAllowCredentials().get().isEmpty()) return false;

            System.out.println("*******************************************************");
            System.out.println(request);
            System.out.println(request.getPublicKeyCredentialRequestOptions().getChallenge());
            System.out.println(request.getPublicKeyCredentialRequestOptions().getAllowCredentials().get().getFirst().getId().getBase64Url().toString());
            System.out.println("user handler byteArray: "+user.getHandle());
            System.out.println("*******************************************************");
            System.out.println(pkc.getResponse().getClientData().getChallenge().getBase64Url().toString());
            System.out.println(pkc.getId());
            System.out.println(pkc.getResponse().getUserHandle());

            System.out.println("*******************************************************");

            // server challange return to server is suppose to be same
            byte[] serverChallenge = request.getPublicKeyCredentialRequestOptions().getChallenge().getBytes();
            byte[] authenticatorChallenge = pkc.getResponse().getClientData().getChallenge().getBytes();

            // allowCredentials and id are suppose to be thesame
            byte[] serverId = request.getPublicKeyCredentialRequestOptions().getAllowCredentials().get().getFirst().getId().getBytes();
            byte[] authenticatorId = pkc.getId().getBytes();

            // handle the userHandle array if they are same
            byte[] serverHandle = user.getHandle().getBytes();
            byte[] authenticatorHandle = pkc.getResponse().getUserHandle().get().getBytes();

            return Arrays.equals(serverChallenge, authenticatorChallenge) && Arrays.equals(serverId, authenticatorId) && Arrays.equals(serverHandle, authenticatorHandle);


            // step 3: include both the request and response the FinishAssertionOptions
            //            FinishAssertionOptions finishAssertionOptions = FinishAssertionOptions.builder()
//                    .request(request)
//                    .response(pkc)
//                    .build();

            // step 4: Perform the finishAssertion to compare the request and response
//            AssertionResult result = relyingParty.finishAssertion(
//                    finishAssertionOptions
//            );

            // return true or false
            //return result.isSuccess();


        } catch (IOException  e) {
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



    private void refreshAuthUserDb() {

        List<String> listOfUsers = authUserRepo.findAllIds();
        List<String> listOfUserSupports = authSupportRepository.findAllIds();
        Set<String> supportUserSet = new HashSet<>(listOfUserSupports);

        for (String userId : listOfUsers) {
            if (!supportUserSet.contains(userId)) {
                authUserRepo.deleteById(userId); // Delete users from listOfUsers not in listOfUserSupports
            }
        }
    }

}
