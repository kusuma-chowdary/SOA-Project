import type { Session } from "./types";

const apiBase = (import.meta.env.VITE_API_URL ?? "http://localhost:8080/api").replace(/\/$/, "");

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

export async function apiRequest<T>(
  path: string,
  session: Session | null,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (session?.accessToken) {
    headers.set("Authorization", `Bearer ${session.accessToken}`);
  }

  let response: Response;
  try {
    response = await fetch(`${apiBase}${path}`, { ...options, headers });
  } catch (requestError) {
    if (requestError instanceof TypeError) {
      throw new ApiError(
        `Cannot reach the Archivalia API at ${apiBase}. Start the Spring Boot services, including the API Gateway on port 8080, then try again.`,
        0,
      );
    }
    throw requestError;
  }
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { detail?: string; message?: string; error?: string } | null;
    throw new ApiError(
      body?.detail ?? body?.message ?? body?.error ?? `The request failed (${response.status}).`,
      response.status,
    );
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}
