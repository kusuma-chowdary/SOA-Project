package com.archivalia.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "catalog_books")
public class Book {
    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 140)
    private String author;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 40)
    private String publishedYear;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String coverUrl;

    @Min(0)
    @Column(nullable = false)
    private int totalCopies;

    @Column(nullable = false)
    private int borrowedCopies;

    @Version
    private long version;

    protected Book() {
    }

    public Book(String title, String author, String category, String publishedYear,
                String description, String coverUrl, int totalCopies) {
        this.title = title;
        this.author = author;
        this.category = category;
        this.publishedYear = publishedYear;
        this.description = description;
        this.coverUrl = coverUrl;
        this.totalCopies = totalCopies;
        this.borrowedCopies = 0;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public String getPublishedYear() { return publishedYear; }
    public String getDescription() { return description; }
    public String getCoverUrl() { return coverUrl; }
    public int getTotalCopies() { return totalCopies; }
    public int getBorrowedCopies() { return borrowedCopies; }
    public int getAvailableCopies() { return totalCopies - borrowedCopies; }

    public void update(String title, String author, String category, String publishedYear,
                       String description, String coverUrl, int totalCopies) {
        if (totalCopies < borrowedCopies) {
            throw new IllegalArgumentException("Total copies cannot be less than the number currently borrowed.");
        }
        this.title = title;
        this.author = author;
        this.category = category;
        this.publishedYear = publishedYear;
        this.description = description;
        this.coverUrl = coverUrl;
        this.totalCopies = totalCopies;
    }

    public void reserveCopy() {
        if (getAvailableCopies() < 1) {
            throw new IllegalStateException("No copies are currently available.");
        }
        borrowedCopies++;
    }

    public void releaseCopy() {
        if (borrowedCopies < 1) {
            throw new IllegalStateException("There are no borrowed copies to return.");
        }
        borrowedCopies--;
    }
}
