package com.shahbazsideprojects.tradematching.repository;

import com.shahbazsideprojects.tradematching.entity.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    @Query("SELECT t FROM Trade t JOIN t.buyOrder bo JOIN t.sellOrder so " +
            "WHERE bo.userId = :userId OR so.userId = :userId ORDER BY t.createdAt DESC")
    Page<Trade> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
