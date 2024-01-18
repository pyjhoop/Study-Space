package com.study.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @EntityGraph(attributePaths = {"members"}, type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT distinct t FROM Team t left join t.members")
    Page<Team> findAllByPage(Pageable pageable);

}
