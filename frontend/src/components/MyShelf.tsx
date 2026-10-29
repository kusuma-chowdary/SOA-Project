import { useState } from "react";
import { ArrowUpRight, RotateCcw } from "lucide-react";
import type { Borrowing, Fine, Session } from "../types";
import { apiRequest } from "../api";

type Props = {
  session: Session;
  borrowings: Borrowing[];
  fines: Fine[];
  onReturned: (borrowing: Borrowing) => void;
};

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export default function MyShelf({ session, borrowings, fines, onReturned }: Props) {
  const [workingId, setWorkingId] = useState("");
  const [error, setError] = useState("");
  const active = borrowings.filter((item) => item.status === "ACTIVE");
  const returned = borrowings.filter((item) => item.status === "RETURNED");
  const assessedTotal = fines.reduce((total, fine) => total + fine.amount, 0);

  async function returnBook(id: string) {
    setWorkingId(id);
    setError("");
    try {
      const updated = await apiRequest<Borrowing>(`/borrowings/${id}/return`, session, { method: "POST" });
      onReturned(updated);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Unable to return this title.");
    } finally {
      setWorkingId("");
    }
  }

  return (
    <section className="shelf-page">
      <div className="page-heading">
        <div><span className="eyebrow">MEMBER SPACE</span><h1>Your reading shelf.</h1></div>
        <p>Keep track of what you are reading, when it is due, and any late-return charges.</p>
      </div>
      <div className="shelf-stats">
        <div><span>Currently borrowed</span><strong>{active.length.toString().padStart(2, "0")}</strong></div>
        <div><span>Titles returned</span><strong>{returned.length.toString().padStart(2, "0")}</strong></div>
        <div><span>Late fees assessed</span><strong>₹{assessedTotal.toFixed(2)}</strong></div>
      </div>
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="section-heading shelf-section-heading">
        <div><span className="eyebrow">ON YOUR DESK</span><h2>Active loans</h2></div>
        <span>{active.length} {active.length === 1 ? "title" : "titles"}</span>
      </div>
      {active.length === 0 ? (
        <div className="empty-state"><span className="empty-mark">✳</span><h3>A little room for a new idea.</h3><p>Your borrowed titles will appear here.</p></div>
      ) : (
        <div className="loan-list">
          {active.map((item) => {
            const isLate = item.dueOn < new Date().toISOString().slice(0, 10);
            return (
              <article className="loan-row" key={item.id}>
                <div className="loan-book-mark"><span>BOOK</span><strong>{item.bookTitle.slice(0, 1)}</strong></div>
                <div className="loan-copy"><h3>{item.bookTitle}</h3><p>{item.bookAuthor}</p></div>
                <div className="loan-date"><span>Due date</span><strong className={isLate ? "late-date" : ""}>{formatDate(item.dueOn)}</strong></div>
                <button className="button button-outline return-button" onClick={() => returnBook(item.id)} disabled={workingId === item.id}>
                  <RotateCcw size={15} />{workingId === item.id ? "Returning…" : "Return"}
                </button>
              </article>
            );
          })}
        </div>
      )}
      <div className="section-heading shelf-section-heading">
        <div><span className="eyebrow">YOUR HISTORY</span><h2>Fine assessments</h2></div>
        <span>{fines.length} records</span>
      </div>
      {fines.length === 0 ? (
        <p className="quiet-note">No late fees have been assessed. Returns on or before the due date are fee-free.</p>
      ) : (
        <div className="fine-list">
          {fines.map((fine) => (
            <div className="fine-row" key={fine.id}>
              <span>{fine.overdueDays} overdue {fine.overdueDays === 1 ? "day" : "days"} · Returned {formatDate(fine.returnedOn)}</span>
              <strong>₹{fine.amount.toFixed(2)}</strong>
              <a href="#discover" aria-label="Browse titles"><ArrowUpRight size={16} /></a>
            </div>
          ))}
        </div>
      )}
      <p className="fine-note">Late fees are assessed at ₹1.00 for each calendar day after the due date.</p>
    </section>
  );
}
