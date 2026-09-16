package com.eventflow.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    private String id; // e.g. ROLE_ADMIN, ROLE_OPERATOR, ROLE_DEVELOPER, ROLE_VIEWER

    @Column(nullable = false, unique = true)
    private String name; // ADMIN, OPERATOR, DEVELOPER, VIEWER

    private String description;

    public Role() {}

    public Role(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
