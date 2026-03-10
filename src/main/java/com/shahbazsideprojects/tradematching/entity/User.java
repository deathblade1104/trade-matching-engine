package com.shahbazsideprojects.tradematching.entity;

import com.shahbazsideprojects.tradematching.constants.TableNames;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = TableNames.USERS.getName(), indexes = {
        @Index(name = "uq_users_email", columnList = "email", unique = true)
})
public class User extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
}
