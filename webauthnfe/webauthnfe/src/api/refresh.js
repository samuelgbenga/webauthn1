import { baseApi } from "./base";

export async function refresh(options = {}) {
  const res = await baseApi.get(
    `api/auth/refresh-table`,
    options
  );
  return res.data;
}
