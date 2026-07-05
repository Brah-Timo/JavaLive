package com.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Simple User JPA entity for the example application.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Constructors ─────────────────────────────────────────────────────────

    public User() {}

    public User(String name, String email) {
        this.name  = name;
        this.email = email;
    }

    // ── Getters and setters ──────────────────────────────────────────────────

    public Long getId()             { return id; }
    public void setId(Long id)      { this.id = id; }

    public String getName()              { return name; }
    public void setName(String name)     { this.name = name; }

    public String getEmail()             { return email; }
    public void setEmail(String email)   { this.email = email; }

    public LocalDateTime getCreatedAt()           { return createdAt; }
    public void setCreatedAt(LocalDateTime c)      { this.createdAt = c; }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}
