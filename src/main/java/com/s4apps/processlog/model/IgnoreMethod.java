package com.s4apps.processlog.model;

import jakarta.persistence.*;

@Entity
@Table(name = "IgnoreMethods")
public class IgnoreMethod {

    @Id
    @Column(name = "method", length = 32, nullable = false)
    private String method;

    @Column(name = "description", length = 200)
    private String description;

    // Getters and setters can be added if you'd like.
}