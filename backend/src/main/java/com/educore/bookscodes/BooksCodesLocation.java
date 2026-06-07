package com.educore.bookscodes;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "books_codes_locations")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BooksCodesLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private LocationType type = LocationType.CENTER;

    private String address;
    private String phone;
    private boolean sellsBooks;
    private boolean sellsCodes;
    private String notes;
    private boolean active = true;
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { this.createdAt = LocalDateTime.now(); }

    public enum LocationType { CENTER, LIBRARY, OTHER }
}
