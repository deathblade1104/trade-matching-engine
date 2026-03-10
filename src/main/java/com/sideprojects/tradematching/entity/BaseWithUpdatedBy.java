package com.sideprojects.tradematching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.LastModifiedBy;

import java.time.Instant;

/**
 * Extends BaseWithCreatedBy with updated_by (signed-in user's email on insert and update).
 * updated_at for entities that need it. Use for entities that need full audit (created_by + updated_by).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseWithUpdatedBy extends BaseWithCreatedBy {

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
