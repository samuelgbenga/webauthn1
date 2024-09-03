import { baseApi } from "./base";

export async function loginStart(username, options = {}) {
  const res = await baseApi.post(
    `api/auth/login/start?username=${username}`,
    options
  );
  return res.data;
}

export async function loginFinish(username, credentials, options = {}) {
  const res = await baseApi.post(
    `api/auth/login/finish?username=${username}`,
    credentials,
    { headers: { "Content-Type": "application/json" }, ...options }
  );
  return res.data;
}
