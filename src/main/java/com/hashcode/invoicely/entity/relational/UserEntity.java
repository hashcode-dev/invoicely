package com.hashcode.invoicely.entity.relational;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "first_name", length = 128)
    private String firstName;

    @Column(name = "last_name", length = 128)
    private String lastName;

    @Column(name = "phone", length = 64)
    private String phone;

    @Column(name = "is_registered", nullable = false)
    @Builder.Default
    private boolean isRegistered = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", length = 32, nullable = false)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.PASSWORD;

    @Column(name = "oauth_id", length = 255)
    private String oauthId;

    @Column(name = "avatar_url", length = 1024)
    private String avatarUrl;

    @Column(name = "organization_id")
    private UUID organizationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
