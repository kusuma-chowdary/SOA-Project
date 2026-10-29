package com.archivalia.book;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SampleCatalogInitializer implements ApplicationRunner {
    private final BookRepository books;

    public SampleCatalogInitializer(BookRepository books) {
        this.books = books;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (books.count() > 0) {
            return;
        }
        books.saveAll(List.of(
                new Book("The Quiet Work of Ideas", "Mira Sen", "Research", "2024",
                        "A thoughtful guide to turning careful questions into meaningful research.", null, 4),
                new Book("Maps for a Changing City", "Arun Mehta", "Urban Studies", "2023",
                        "How neighbourhoods, memory and public space shape the cities we share.", null, 3),
                new Book("The Shape of Small Things", "Leela Nair", "Literature", "2022",
                        "Short essays on observation, language and the everyday details worth keeping.", null, 2),
                new Book("Notes from the Living Earth", "Dev Patel", "Environment", "2025",
                        "An accessible introduction to biodiversity, conservation and local action.", null, 5),
                new Book("A Commons of Knowledge", "Anika Rao", "Education", "2024",
                        "A practical study of open learning, public collections and shared scholarship.", null, 3),
                new Book("Patterns of Belonging", "Rohan Iyer", "Social Sciences", "2021",
                        "Research on identity, community and the stories people tell about home.", null, 2),
                new Book("Reading the Archive", "Nadia Thomas", "History", "2023",
                        "A beginner-friendly companion to primary sources, catalogues and historical method.", null, 4),
                new Book("The Patient Experiment", "Ishaan Bose", "Science", "2025",
                        "Clear ideas for building reliable experiments and learning from unexpected results.", null, 3)
        ));
    }
}
