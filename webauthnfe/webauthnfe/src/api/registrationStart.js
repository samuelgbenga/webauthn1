import { baseApi } from "./base";

export async function registerStart(username, options = {}) {
  const res = await baseApi.post(
    `api/auth/register/start?username=${username}`,
    options
  );
  return res.data;
}

export async function registerFinish(username, credentials, options = {}) {
  const res = await baseApi.post(
    `api/auth/register/finish?username=${username}`,
    credentials,
    { headers: { "Content-Type": "application/json" }, ...options }
  );
  return res.data;
}
