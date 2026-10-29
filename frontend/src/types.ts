export type User = {
  userId: string;
  name: string;
  email: string;
  role: "USER" | "ADMIN";
};

export type Session = User & {
  accessToken: string;
  expiresInSeconds: number;
};

export type Book = {
  id: string;
  title: string;
  author: string;
  category: string;
  publishedYear: string | null;
  description: string | null;
  coverUrl: string | null;
  totalCopies: number;
  borrowedCopies: number;
  availableCopies: number;
};

export type Borrowing = {
  id: string;
  bookId: string;
  bookTitle: string;
  bookAuthor: string;
  borrowedOn: string;
  dueOn: string;
  returnedOn: string | null;
  status: "ACTIVE" | "RETURNED";
  fineAmount: number;
  overdueDays: number;
};

export type Fine = {
  id: string;
  borrowingId: string;
  dueOn: string;
  returnedOn: string;
  amount: number;
  overdueDays: number;
};
