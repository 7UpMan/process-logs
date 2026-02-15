package com.s4apps.processlog.model;

import jakarta.persistence.*;

@Entity
@Table(name = "IgnoreUrls")
public class IgnoreUrl {

    @Id
    @Column(name = "url", length = 32, nullable = false)
    private String url;

    @Column(name = "description", length = 200)
    private String description;

    // Getters and setters can be generated if you'd like.
}