package com.social_navigator_ar.demo.repository;

import com.social_navigator_ar.demo.model.ImportedVacancy;
import com.social_navigator_ar.demo.model.ImportedVacancy.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ImportedVacancyRepository extends JpaRepository<ImportedVacancy, Long> {
    boolean existsBySourceAndExternalId(String source, String externalId);
    List<ImportedVacancy> findByStatusNotInOrderByFoundAtDesc(Collection<Status> statuses);
    long countByStatus(Status status);
}
