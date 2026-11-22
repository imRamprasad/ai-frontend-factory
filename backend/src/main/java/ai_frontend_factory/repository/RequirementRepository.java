package ai_frontend_factory.repository;

import ai_frontend_factory.entity.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RequirementRepository extends JpaRepository<Requirement, String> {

    @Query("""
            select requirement
            from Requirement requirement
            where requirement.project.id = :projectId
            order by requirement.createdAt desc
            """)
    List<Requirement> findByProjectIdOrderByCreatedAtDesc(@Param("projectId") String projectId);

    @Query("""
            select requirement
            from Requirement requirement
            where requirement.id = :requirementId
              and requirement.project.id = :projectId
            """)
    Optional<Requirement> findByIdAndProjectId(
            @Param("requirementId") String requirementId,
            @Param("projectId") String projectId
    );
}
