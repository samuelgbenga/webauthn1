import React, { useEffect, useRef, useState } from "react";
import { getTesting } from "./api/getTesting";
import { registerFinish, registerStart } from "./api/registrationStart";
import { loginFinish, loginStart } from "./api/loginStart";
import QRCode from "qrcode";

const App = () => {
  const [resp, setResp] = useState({});
  const [show, setShow] = useState(false);
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

  // TEst registration code
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
          id: "localhost",
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
        id: credential.id.replace(/_/g, "/").replace(/-/g, "+"),
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
      const jsonString = JSON.stringify(credentialResponse);
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

      const data = await response;
      //const data = await response;

      //console.log(data);
      const publicKey = data.key; // This is the challenge data

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
        userVerification: "preferred",
      };

      //step 3 get the challenge from the authenticator device second
      const credential = await navigator.credentials.get({
        publicKey: assertionOptions,
      });

      // step 4 structure the credential in a acceptable
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
          userHandle: base64UrlEncode(new Uint8Array(credential.id)),
          // userHandle: credential.response.userHandle
          //   ? base64UrlEncode(new Uint8Array(credential.response.userHandle))
          //   : null, // Handle null or undefined cases
        },

        clientExtensionResults: credential.getClientExtensionResults(),
        type: credential.type,
      };

      const jsonString = JSON.stringify(credentialResponse);
      // console.log(jsonString);

      // step 5 send the challenge or credential back to the server
      const result = await loginFinish(username, jsonString);

      // sixt division
      if (result.ok) {
        setMessage("Login successful");
        console.log("login successful");
      } else {
        setMessage("Login failed");
        console.log("Login Failed");
      }
    } catch (error) {
      console.error("Login failed:", error);
      setMessage("Login failed");
    }
  };

  useEffect(() => {
    console.log(resp);
  }, [resp]);

  return (
    <div>
      <button onClick={handleGetMe}>Get me button</button>
      <div>{resp && <pre>{JSON.stringify(resp, null, 2)}</pre>}</div>

      <br />
      <br />
      <br />

      <div>
        <form onSubmit={handleRegister}>
          <label>
            <input type="text" ref={usernameRef} />
          </label>
          <button type="submit">Register Now</button>
        </form>
      </div>

      <br />
      <br />
      <br />

      <div>
        <div>Login</div>
        <form onSubmit={handleLogin}>
          <label>
            <input type="text" ref={usernameRef1} />
          </label>
          <button type="submit">Login</button>
        </form>
      </div>
    </div>
  );
};

export default App;
