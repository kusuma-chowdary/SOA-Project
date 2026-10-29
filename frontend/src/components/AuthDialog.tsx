import { useState, type FormEvent } from "react";
import { X } from "lucide-react";
import type { Session } from "../types";
import { apiRequest } from "../api";

type Props = {
  onClose: () => void;
  onSignedIn: (session: Session) => void;
};

export default function AuthDialog({ onClose, onSignedIn }: Props) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setBusy(true);
    const values = new FormData(event.currentTarget);
    const payload = Object.fromEntries(values.entries());
    try {
      const session = await apiRequest<Session>(`/auth/${mode}`, null, {
        method: "POST",
        body: JSON.stringify(payload),
      });
      onSignedIn(session);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to sign in.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="dialog-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose();
    }}>
      <section className="auth-dialog" role="dialog" aria-modal="true" aria-labelledby="auth-title">
        <button className="icon-button dialog-close" onClick={onClose} aria-label="Close dialog"><X /></button>
        <span className="eyebrow">YOUR READING ROOM</span>
        <h2 id="auth-title">{mode === "login" ? "Welcome back." : "Make room for ideas."}</h2>
        <p className="muted">{mode === "login"
          ? "Sign in to manage your borrowed titles."
          : "Create an institutional library account."}</p>
        <form className="stack-form" onSubmit={submit}>
          {mode === "register" && (
            <label>
              Full name
              <input name="name" autoComplete="name" required minLength={2} maxLength={80} />
            </label>
          )}
          <>
            <label>
              Institutional email
              <input name="email" type="email" autoComplete="email" required />
            </label>
            <label>
              Password
              <input name="password" type="password" autoComplete={mode === "login" ? "current-password" : "new-password"} required minLength={mode === "register" ? 8 : undefined} />
            </label>
          </>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="button button-dark" disabled={busy}>
            {busy ? "One moment…" : mode === "login" ? "Continue" : "Create account"}
          </button>
        </form>
        <p className="auth-switch">
          {mode === "login" ? "New to Archivalia?" : "Already have an account?"}{" "}
          <button className="text-button" onClick={() => { setError(""); setMode(mode === "login" ? "register" : "login"); }}>
            {mode === "login" ? "Create an account" : "Sign in"}
          </button>
        </p>
      </section>
    </div>
  );
}
