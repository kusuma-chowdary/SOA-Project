import { BookOpen, Check, Clock3 } from "lucide-react";
import type { Book } from "../types";

type Props = {
  book: Book;
  onBorrow: (book: Book) => void;
  signedIn: boolean;
};

const coverColors = ["cover-olive", "cover-coral", "cover-blue", "cover-plum", "cover-gold"];

export default function BookCard({ book, onBorrow, signedIn }: Props) {
  const coverClass = coverColors[book.title.length % coverColors.length];

  return (
    <article className="book-card">
      <div className={`book-cover ${coverClass}`}>
        {book.coverUrl ? (
          <img src={book.coverUrl} alt={`Cover of ${book.title}`} loading="lazy" />
        ) : (
          <div className="cover-art">
            <BookOpen size={22} strokeWidth={1.5} />
            <span>ARCHIVALIA<br />READING SERIES</span>
            <strong>{book.title}</strong>
            <small>{book.author}</small>
          </div>
        )}
        <span className="cover-category">{book.category}</span>
      </div>
      <div className="book-card-body">
        <div className="book-meta">
          <span>{book.publishedYear || "Digital edition"}</span>
          <span className={book.availableCopies > 0 ? "availability" : "availability unavailable"}>
            {book.availableCopies > 0 ? <Check size={13} /> : <Clock3 size={13} />}
            {book.availableCopies > 0 ? `${book.availableCopies} available` : "On loan"}
          </span>
        </div>
        <h3>{book.title}</h3>
        <p className="book-author">by {book.author}</p>
        {book.description && <p className="book-description">{book.description}</p>}
        <button
          className="button button-outline borrow-button"
          disabled={book.availableCopies < 1}
          onClick={() => onBorrow(book)}
        >
          {signedIn ? "Borrow this title" : "Sign in to borrow"}
          <span aria-hidden="true">↗</span>
        </button>
      </div>
    </article>
  );
}
