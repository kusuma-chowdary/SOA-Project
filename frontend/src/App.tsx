import { useEffect, useMemo, useState } from "react";
import { ArrowDown, ArrowRight, BookMarked, BookOpen, Search, ShieldCheck, Sparkles, UserRound } from "lucide-react";
import { ApiError, apiRequest } from "./api";
import AccountSettingsDialog from "./components/AccountSettingsDialog";
import AdminBookForm from "./components/AdminBookForm";
import AuthDialog from "./components/AuthDialog";
import BookCard from "./components/BookCard";
import MyShelf from "./components/MyShelf";
import type { Book, Borrowing, Fine, Session } from "./types";

type Page = "discover" | "shelf";
const sessionStorageKey = "archivalia-session";

function readSession(): Session | null {
  const saved = localStorage.getItem(sessionStorageKey);
  if (!saved) return null;
  try {
    return JSON.parse(saved) as Session;
  } catch {
    localStorage.removeItem(sessionStorageKey);
    return null;
  }
}

export default function App() {
  const [session, setSession] = useState<Session | null>(readSession);
  const [page, setPage] = useState<Page>("discover");
  const [showAuth, setShowAuth] = useState(false);
  const [showAccountSettings, setShowAccountSettings] = useState(false);
  const [books, setBooks] = useState<Book[]>([]);
  const [borrowings, setBorrowings] = useState<Borrowing[]>([]);
  const [fines, setFines] = useState<Fine[]>([]);
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("All titles");
  const [loadingBooks, setLoadingBooks] = useState(true);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const query = search.trim() ? `?q=${encodeURIComponent(search.trim())}` : "";
      setLoadingBooks(true);
      apiRequest<Book[]>(`/books${query}`, session)
        .then((catalog) => {
          setBooks(catalog);
          setError("");
        })
        .catch((requestError: unknown) => {
          setError(requestError instanceof Error ? requestError.message : "The catalogue could not be loaded.");
        })
        .finally(() => setLoadingBooks(false));
    }, search.trim() ? 200 : 0);
    return () => window.clearTimeout(timer);
  }, [search, session]);

  useEffect(() => {
    if (!session) {
      setBorrowings([]);
      setFines([]);
      return;
    }
    apiRequest<Borrowing[]>("/borrowings/mine", session)
      .then(setBorrowings)
      .catch((requestError: unknown) => {
        setError(requestError instanceof Error ? requestError.message : "Your reading shelf could not be loaded.");
      });
    apiRequest<Fine[]>("/fines/mine", session)
      .then(setFines)
      .catch((requestError: unknown) => {
        setError(requestError instanceof Error ? requestError.message : "Fine assessments could not be loaded.");
      });
  }, [session]);

  const categories = useMemo(
    () => ["All titles", ...Array.from(new Set(books.map((book) => book.category))).sort()],
    [books],
  );
  const visibleBooks = category === "All titles"
    ? books
    : books.filter((book) => book.category === category);

  function signIn(nextSession: Session) {
    localStorage.setItem(sessionStorageKey, JSON.stringify(nextSession));
    setSession(nextSession);
    setShowAuth(false);
    setNotice(`Welcome to your reading room, ${nextSession.name.split(" ")[0]}.`);
    setError("");
  }

  function updateSession(nextSession: Session) {
    localStorage.setItem(sessionStorageKey, JSON.stringify(nextSession));
    setSession(nextSession);
    setShowAccountSettings(false);
    setNotice("Your account details have been updated.");
    setError("");
  }

  function signOut() {
    localStorage.removeItem(sessionStorageKey);
    setSession(null);
    setPage("discover");
    setNotice("You have been signed out.");
  }

  async function borrowBook(book: Book) {
    if (!session) {
      setShowAuth(true);
      return;
    }
    try {
      const borrowing = await apiRequest<Borrowing>("/borrowings", session, {
        method: "POST",
        body: JSON.stringify({ bookId: book.id }),
      });
      setBorrowings((current) => [borrowing, ...current]);
      setBooks((current) => current.map((item) => item.id === book.id
        ? { ...item, availableCopies: item.availableCopies - 1, borrowedCopies: item.borrowedCopies + 1 }
        : item));
      setNotice(`“${book.title}” is on your shelf. It is due ${new Date(`${borrowing.dueOn}T00:00:00`).toLocaleDateString("en-IN", { day: "numeric", month: "short" })}.`);
      setError("");
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        setShowAuth(true);
      } else {
        setError(requestError instanceof Error ? requestError.message : "This title could not be borrowed.");
      }
    }
  }

  function recordReturn(updated: Borrowing) {
    setBorrowings((current) => current.map((item) => item.id === updated.id ? updated : item));
    setBooks((current) => current.map((item) => item.id === updated.bookId
      ? { ...item, availableCopies: item.availableCopies + 1, borrowedCopies: Math.max(0, item.borrowedCopies - 1) }
      : item));
    if (session) {
      apiRequest<Fine[]>("/fines/mine", session).then(setFines).catch((requestError: unknown) => {
        setError(requestError instanceof Error ? requestError.message : "Fine assessments could not be refreshed.");
      });
    }
    setNotice(updated.fineAmount > 0
      ? `Returned. The late fee is ₹${updated.fineAmount.toFixed(2)}.`
      : "Thank you for returning your title on time.");
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <a className="brand" href="#discover" onClick={() => setPage("discover")} aria-label="Archivalia home">
          <span className="brand-mark"><BookOpen size={20} strokeWidth={1.7} /></span>
          <span>archivalia<span className="brand-dot">.</span><small>THE DIGITAL READING ROOM</small></span>
        </a>
        <nav className="main-nav" aria-label="Main navigation">
          <button className={page === "discover" ? "nav-link active" : "nav-link"} onClick={() => setPage("discover")}>Discover</button>
          {session && <button className={page === "shelf" ? "nav-link active" : "nav-link"} onClick={() => setPage("shelf")}>My shelf</button>}
        </nav>
        <div className="header-account">
          {session ? (
            <>
              <span className="user-greeting"><span className="online-dot" />{session.name.split(" ")[0]}</span>
              <button className="button button-outline header-button" onClick={() => setShowAccountSettings(true)}>
                <UserRound size={14} /> Account
              </button>
              <button className="button button-dark header-button" onClick={signOut}>Sign out</button>
            </>
          ) : (
            <button className="button button-dark header-button" onClick={() => setShowAuth(true)}>Member sign in <ArrowRight size={15} /></button>
          )}
        </div>
      </header>

      <main>
        {page === "discover" ? (
          <>
            <section className="hero">
              <div className="hero-copy">
                <span className="eyebrow"><span className="eyebrow-line" /> A LIBRARY WITHOUT WALLS</span>
                <h1>Good ideas<br />deserve <em>room.</em></h1>
                <p>A considered collection of books and scholarship, ready when curiosity finds you.</p>
                <a className="hero-link" href="#catalogue">Explore the collection <ArrowDown size={16} /></a>
                <div className="hero-footnote"><ShieldCheck size={15} /> For curious minds &amp; academic communities</div>
              </div>
              <div className="hero-art" aria-label="A quiet corner for reading">
                <div className="sun-shape" />
                <div className="window-frame"><div className="window-sky" /><div className="window-horizon" /></div>
                <div className="plant plant-one"><span /><i /><b /></div>
                <div className="hero-books">
                  <span className="hero-book book-one">FIELD<br />NOTES</span>
                  <span className="hero-book book-two">THE<br />COMMON<br />GOOD</span>
                  <span className="hero-book book-three">WAYS OF<br />SEEING</span>
                  <span className="book-stack" />
                </div>
                <span className="art-caption">A SMALL PAUSE, A NEW PERSPECTIVE.</span>
                <span className="art-sparkle"><Sparkles size={18} /></span>
              </div>
              <div className="hero-index">01 <span /> 04</div>
            </section>

            <section className="catalogue-section" id="catalogue">
              <div className="catalogue-topline">
                <div className="section-heading">
                  <div><span className="eyebrow">THE COLLECTION</span><h2>Find your next <em>something.</em></h2></div>
                  <p>Thoughtful reads for the questions that stay with you.</p>
                </div>
                <div className="catalogue-search">
                  <Search size={17} />
                  <input aria-label="Search books" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search title, author or subject" />
                  <kbd>⌕</kbd>
                </div>
              </div>
              <div className="category-row" aria-label="Filter by category">
                <span className="category-label">BROWSE BY</span>
                {categories.map((item) => (
                  <button className={category === item ? "category-chip selected" : "category-chip"} key={item} onClick={() => setCategory(item)}>{item}</button>
                ))}
                <span className="catalogue-count"><BookMarked size={14} /> {visibleBooks.length} titles</span>
              </div>
              {session?.role === "ADMIN" && <AdminBookForm session={session} onAdded={(book) => setBooks((current) => [book, ...current])} />}
              {notice && <div className="notice" role="status">{notice}<button onClick={() => setNotice("")} aria-label="Dismiss message">×</button></div>}
              {error && <div className="notice notice-error" role="alert">{error}<button onClick={() => setError("")} aria-label="Dismiss error">×</button></div>}
              {loadingBooks ? (
                <div className="loading-state"><span className="loading-spinner" /> Gathering the collection…</div>
              ) : visibleBooks.length > 0 ? (
                <div className="book-grid">
                  {visibleBooks.map((book) => <BookCard key={book.id} book={book} onBorrow={borrowBook} signedIn={Boolean(session)} />)}
                </div>
              ) : (
                <div className="empty-state"><span className="empty-mark">⌕</span><h3>No titles found just yet.</h3><p>Try another search or choose a different subject.</p></div>
              )}
              <div className="catalogue-endmark"><span /> END OF CURRENT COLLECTION <span /></div>
            </section>

            <section className="reading-note">
              <div className="note-symbol">“</div>
              <div><span className="eyebrow">A NOTE ON READING</span><blockquote>A good library isn't about having all the answers. It's about making more questions possible.</blockquote><span className="note-credit">THE ARCHIVALIA READING ROOM</span></div>
              <div className="note-decoration"><span /><span /><span /></div>
            </section>
          </>
        ) : session ? (
          <MyShelf
            session={session}
            borrowings={borrowings}
            fines={fines}
            onReturned={recordReturn}
          />
        ) : null}
      </main>

      <footer className="site-footer">
        <a className="brand footer-brand" href="#discover" onClick={() => setPage("discover")}>
          <span className="brand-mark"><BookOpen size={18} /></span>
          <span>archivalia<span className="brand-dot">.</span><small>THE DIGITAL READING ROOM</small></span>
        </a>
        <p>For the things you haven't thought of yet.</p>
        <span className="footer-note">MADE FOR THE LIFE OF THE MIND · {new Date().getFullYear()}</span>
      </footer>
      {showAuth && <AuthDialog onClose={() => setShowAuth(false)} onSignedIn={signIn} />}
      {showAccountSettings && session && (
        <AccountSettingsDialog
          session={session}
          onClose={() => setShowAccountSettings(false)}
          onUpdated={updateSession}
        />
      )}
      <button className="floating-library" onClick={() => setPage("discover")} aria-label="Back to the catalogue"><BookOpen size={17} /></button>
    </div>
  );
}
