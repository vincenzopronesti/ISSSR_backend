package com.isssr.ticketing_system.dao;

import com.isssr.ticketing_system.entity.Target;
import com.isssr.ticketing_system.enumeration.TargetState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TargetDao extends JpaRepository<Target, Long> {
    Target findByName(String name);

    boolean existsByName(String name);

    @Query("select t from Target t where t.targetState = :state")
    List<Target> getActiveTarget(@Param("state") TargetState targetState);
}