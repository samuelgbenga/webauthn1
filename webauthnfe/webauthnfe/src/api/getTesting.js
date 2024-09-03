import { baseApi } from "./base";

export async function getTesting(options = {}) {
  const res = await baseApi.get(
    `api/auth/testing`,
    options
  );
  return res.data;
}
