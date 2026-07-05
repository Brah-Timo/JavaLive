package com.example.service;

import com.example.model.User;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple in-memory UserService for the example application.
 * In a real app, inject a Spring Data repository here.
 */
@Service
public class UserService {

    private final List<User> users = new ArrayList<>(List.of(
        new User("Alice Johnson",  "alice@example.com"),
        new User("Bob Smith",      "bob@example.com"),
        new User("Charlie Brown",  "charlie@example.com"),
        new User("Diana Prince",   "diana@example.com"),
        new User("Edward Norton",  "edward@example.com")
    ));

    private long nextId = 6L;

    public UserService() {
        for (int i = 0; i < users.size(); i++) {
            users.get(i).setId((long)(i + 1));
        }
    }

    public List<User> findAll(int page, int pageSize) {
        int start = (page - 1) * pageSize;
        int end   = Math.min(start + pageSize, users.size());
        if (start >= users.size()) return Collections.emptyList();
        return new ArrayList<>(users.subList(start, end));
    }

    public List<User> search(String query, int page, int pageSize) {
        if (query == null || query.isEmpty()) return findAll(page, pageSize);
        String q = query.toLowerCase();
        List<User> filtered = users.stream()
            .filter(u -> u.getName().toLowerCase().contains(q)
                      || u.getEmail().toLowerCase().contains(q))
            .collect(Collectors.toList());
        int start = (page - 1) * pageSize;
        int end   = Math.min(start + pageSize, filtered.size());
        if (start >= filtered.size()) return Collections.emptyList();
        return new ArrayList<>(filtered.subList(start, end));
    }

    public int getTotalPages(int pageSize) {
        return (int) Math.ceil((double) users.size() / pageSize);
    }

    public int getSearchTotalPages(String query, int pageSize) {
        if (query == null || query.isEmpty()) return getTotalPages(pageSize);
        String q = query.toLowerCase();
        long count = users.stream()
            .filter(u -> u.getName().toLowerCase().contains(q)
                      || u.getEmail().toLowerCase().contains(q))
            .count();
        return (int) Math.ceil((double) count / pageSize);
    }

    public Optional<User> findById(Long id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    public User save(String name, String email) {
        User user = new User(name, email);
        user.setId(nextId++);
        users.add(user);
        return user;
    }

    public void delete(Long id) {
        users.removeIf(u -> u.getId().equals(id));
    }

    public int count() { return users.size(); }
}
