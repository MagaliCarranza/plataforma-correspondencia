import { api } from "./api";

export async function obtenerDemoInfo() {
  const { data } = await api.get("/demo/info");
  return data;
}
