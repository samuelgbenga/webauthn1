import React, { useEffect, useRef, useState } from "react";
import { getTesting } from "./api/getTesting";
import { registerFinish, registerStart } from "./api/registrationStart";
import { loginFinish, loginStart } from "./api/loginStart";
import "./App.css"; // Import the CSS file

const App = () => {
  const [resp, setResp] = useState({});
  //const [show, setShow] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState();
  const usernameRef = useRef();
  const usernameRef1 = useRef();
  const [message, setMessage] = useState("");

  function base64UrlEncode(bytes) {
    // Convert bytes to Base64 string
    let base64 = btoa(String.fromCharCode.apply(null, new Uint8Array(bytes)));
    // Replace Base64 characters with URL-safe characters
    return base64.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  }

  const handleGetMe = async () => {
    setIsLoading(true);
    try {
      const response = await getTesting();

      setResp(response);
    } catch (error) {
      console.log("this error", error);
      setErrorMessage("An error occur");
    } finally {
      setIsLoading(false);
    }

    // setShow(!show);
  };

  // // TEst registration code
  const handleRegister = async (e) => {
    e.preventDefault();

    const username = usernameRef.current.value;

    try {
      //step 1 get the server challenge
      const response = await registerStart(username);

      const data = await response;

      const publicKey = data.key; // This is the challenge data

      //step 2 send the challenge to the plateform Authenticator
      const credentialCreationOptions = {
        challenge: Uint8Array.from(
          atob(
            publicKey.publicKey.challenge.replace(/_/g, "/").replace(/-/g, "+")
          ),
          (c) => c.charCodeAt(0)
        ),
        rp: {
          name: "webauthn1",
          id: "webauthn-demo-4mpz.onrender.com",
        },
        user: {
          id: Uint8Array.from(username, (c) => c.charCodeAt(0)),
          name: username,
          displayName: username,
        },
        pubKeyCredParams: [
          { type: "public-key", alg: -7 }, // ES256
          { type: "public-key", alg: -257 }, // RS256
        ],
        // authenticatorSelection: {
        //   authenticatorAttachment: "platform",
        // },
        timeout: 60000,
        attestation: "direct",
      };

      //step 3 get the challenge from the authenticator device second
      const credential = await navigator.credentials.create({
        publicKey: credentialCreationOptions,
      });

      // step 4 structure the credential in a acceptable
      //formate that would be sent to the server
      const credentialResponse = {
        id: credential.id,
        //rawId: Array.from(new Uint8Array(credential.rawId)),
        //rawId:  base64UrlEncode(new Uint8Array(credential.rawId)),
        response: {
          attestationObject: base64UrlEncode(
            new Uint8Array(credential.response.attestationObject)
          ),
          clientDataJSON: base64UrlEncode(
            new Uint8Array(credential.response.clientDataJSON)
          ),
          //userHandle:  base64UrlEncode(new Uint8Array(credential.response.userHandler))
        },
        clientExtensionResults: credential.getClientExtensionResults(),
        type: credential.type,
      };
      //console.log(credentialResponse)
      //console.log(jsonString)

      // step 5 send the challenge or credential back to the server
      const autResponse = await registerFinish(username, credentialResponse);

      console.log(autResponse);
      setMessage("Registration successful");
    } catch (error) {
      console.log("Registration failed:", error);
      setMessage("Registration failed");
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();

    const username = usernameRef1.current.value;

    try {
      //step 1 get the server challenge
      const response = await loginStart(username);

      //const data = await response;
      const data = await response;

      //console.log(data);
      const publicKey = data.key; // This is the challenge data

      console.log(publicKey.publicKey.allowCredentials[0].id);
      //step 2 send the challenge to the plateform Authenticator
      const assertionOptions = {
        challenge: Uint8Array.from(
          atob(
            publicKey.publicKey.challenge.replace(/_/g, "/").replace(/-/g, "+")
          ),
          (c) => c.charCodeAt(0)
        ),
        rpId: publicKey.publicKey.rpId,
        allowCredentials: publicKey.publicKey.allowCredentials.map((cred) => ({
          type: cred.type,
          id: Uint8Array.from(
            atob(cred.id.replace(/-/g, "+").replace(/_/g, "/")),
            (c) => c.charCodeAt(0)
          ),
        })),
        timeout: 60000,
        userVerification: publicKey.publicKey.userVerification,
      };

      // console.log(assertionOptions);
      //step 3 get the challenge from the authenticator device second
      const credential = await navigator.credentials.get({
        publicKey: assertionOptions,
      });

      //data.handle = atob(data.handle.replace(/-/g, "+").replace(/_/g, "/"));
      let str = data.handle;
      let str1 = str.replace(/-/g, "+").replace(/_/g, "/").replace(/=+$/, "");

      console.log("from authenticator: ", str1);

      // Step 4: Structure the credential in an acceptable format
      const credentialResponse = {
        id: credential.id,
        response: {
          authenticatorData: base64UrlEncode(
            new Uint8Array(credential.response.authenticatorData)
          ),
          clientDataJSON: base64UrlEncode(
            new Uint8Array(credential.response.clientDataJSON)
          ),
          signature: base64UrlEncode(
            new Uint8Array(credential.response.signature)
          ),
          userHandle: credential.response.userHandle
            ? base64UrlEncode(new Uint8Array(credential.response.userHandle))
            : base64UrlEncode(
                Uint8Array.from(atob(str1), (c) => c.charCodeAt(0))
              ),
        },
        clientExtensionResults: credential.getClientExtensionResults(),
        type: credential.type,
      };

      // step 5 send the challenge or credential back to the server
      const result = await loginFinish(username, credentialResponse);

      // sixt division
      if (result) {
        setMessage("Login successful");
        console.log("login successful");
      } else {
        setMessage("Login failed");
        console.log("Login Failed");
      }
    } catch (error) {
      console.log("Login failed:", error);
      setMessage("Login failed");
    }
  };

  useEffect(() => {
    console.log(resp);
  }, [resp]);

  return (
    <div className="app-container">
      <button onClick={handleGetMe}>
        {isLoading ? "Loading..." : "Get me button"}
      </button>

      <div>{resp && <pre>{JSON.stringify(resp, null, 2)}</pre>}</div>

      {errorMessage && <div className="error-message">{errorMessage}</div>}

      <div className="form-container">
        <h2>Register</h2>
        <form onSubmit={handleRegister}>
          <input type="text" ref={usernameRef} placeholder="Enter username" />
          <button type="submit">Register Now</button>
        </form>
        {message && (
          <div
            className={
              message.includes("failed") ? "error-message" : "success-message"
            }
          >
            {message}
          </div>
        )}
      </div>

      <div className="form-container">
        <h2>Login</h2>
        <form onSubmit={handleLogin}>
          <input type="text" ref={usernameRef1} placeholder="Enter username" />
          <button type="submit">Login</button>
        </form>
      </div>
    </div>
  );
};

export default App;

