import { useState, type FormEvent } from "react";
import type { Book, Session } from "../types";
import { apiRequest } from "../api";

type Props = {
  session: Session;
  onAdded: (book: Book) => void;
};

export default function AdminBookForm({ session, onAdded }: Props) {
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    setError("");
    setBusy(true);
    const form = event.currentTarget;
    const values = new FormData(form);
    const payload = {
      title: String(values.get("title") ?? ""),
      author: String(values.get("author") ?? ""),
      category: String(values.get("category") ?? ""),
      publishedYear: String(values.get("publishedYear") ?? ""),
      description: String(values.get("description") ?? ""),
      totalCopies: Number(values.get("totalCopies") ?? 0),
    };
    try {
      const book = await apiRequest<Book>("/books", session, {
        method: "POST",
        body: JSON.stringify(payload),
      });
      onAdded(book);
      form.reset();
      setMessage("The title has been added to the catalogue.");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to add this title.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <details className="admin-panel">
      <summary>Catalogue tools <span>Administrator</span></summary>
      <form className="admin-form" onSubmit={submit}>
        <label>Title<input name="title" required maxLength={180} /></label>
        <label>Author<input name="author" required maxLength={140} /></label>
        <label>Category<input name="category" required maxLength={100} placeholder="Research, Literature…" /></label>
        <label>Year<input name="publishedYear" maxLength={40} /></label>
        <label>Copies<input name="totalCopies" type="number" min="0" defaultValue="1" required /></label>
        <label className="admin-description">Description<textarea name="description" rows={2} maxLength={1000} /></label>
        <button className="button button-dark" disabled={busy}>{busy ? "Adding…" : "Add title"}</button>
      </form>
      {message && <p className="form-success" role="status">{message}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}
    </details>
  );
}
