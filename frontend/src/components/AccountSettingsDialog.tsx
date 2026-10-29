import { useState, type FormEvent } from "react";
import { X } from "lucide-react";
import { apiRequest } from "../api";
import type { Session } from "../types";

type Props = {
  session: Session;
  onClose: () => void;
  onUpdated: (session: Session) => void;
};

export default function AccountSettingsDialog({ session, onClose, onUpdated }: Props) {
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    const values = new FormData(event.currentTarget);
    const newPassword = String(values.get("newPassword") ?? "");
    const confirmPassword = String(values.get("confirmPassword") ?? "");
    if (newPassword !== confirmPassword) {
      setError("New passwords do not match.");
      return;
    }

    setBusy(true);
    try {
      const updatedSession = await apiRequest<Session>("/auth/me", session, {
        method: "PUT",
        body: JSON.stringify({
          currentPassword: values.get("currentPassword"),
          email: values.get("email"),
          newPassword,
        }),
      });
      onUpdated(updatedSession);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Account changes could not be saved.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="dialog-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose();
    }}>
      <section className="auth-dialog" role="dialog" aria-modal="true" aria-labelledby="account-title">
        <button className="icon-button dialog-close" onClick={onClose} aria-label="Close account settings"><X /></button>
        <span className="eyebrow">YOUR READING ROOM</span>
        <h2 id="account-title">Account settings.</h2>
        <p className="muted">Confirm your current password to save account changes.</p>
        <form className="stack-form" onSubmit={submit}>
          <label>
            Email address
            <input name="email" type="email" autoComplete="email" defaultValue={session.email} required maxLength={160} />
          </label>
          <label>
            Current password
            <input name="currentPassword" type="password" autoComplete="current-password" required />
          </label>
          <label>
            New password <span className="muted">(leave blank to keep it)</span>
            <input name="newPassword" type="password" autoComplete="new-password" minLength={8} maxLength={100} />
          </label>
          <label>
            Confirm new password
            <input name="confirmPassword" type="password" autoComplete="new-password" minLength={8} maxLength={100} />
          </label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="button button-dark" disabled={busy}>
            {busy ? "Saving…" : "Save changes"}
          </button>
        </form>
      </section>
    </div>
  );
}
