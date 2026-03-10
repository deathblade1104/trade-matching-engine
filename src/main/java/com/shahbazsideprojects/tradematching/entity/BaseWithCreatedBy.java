package com.shahbazsideprojects.tradematching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;

/**
 * Extends BaseEntity with created_by (signed-in user's email on insert).
 * Use for entities that need to track who created the record.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseWithCreatedBy extends BaseEntity {

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;
}
