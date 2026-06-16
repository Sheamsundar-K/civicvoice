package com.civicvoice.issue.repository;

import com.civicvoice.issue.domain.Issue;
import com.civicvoice.issue.domain.IssueCategory;
import com.civicvoice.issue.domain.IssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {

    // ─── Geo-proximity query using Haversine (no PostGIS required) ───────────────
    /**
     * Find all issues within a given radius (in meters) using bounding-box pre-filter
     * and Haversine formula for accurate distance check.
     */
    @Query(value = """
        SELECT i.* FROM issues i
        WHERE (
            6371000 * acos(
                cos(radians(:lat)) * cos(radians(i.latitude))
                * cos(radians(i.longitude) - radians(:lng))
                + sin(radians(:lat)) * sin(radians(i.latitude))
            )
        ) <= :radiusMeters
        ORDER BY (
            6371000 * acos(
                cos(radians(:lat)) * cos(radians(i.latitude))
                * cos(radians(i.longitude) - radians(:lng))
                + sin(radians(:lat)) * sin(radians(i.latitude))
            )
        )
        """, nativeQuery = true)
    List<Issue> findNearby(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radiusMeters") double radiusMeters
    );

    // ─── Duplicate detection: same category + within radius ──────────────────
    @Query(value = """
        SELECT i.* FROM issues i
        WHERE i.category = :category
          AND i.status NOT IN ('RESOLVED', 'CLOSED', 'REJECTED')
          AND (
            6371000 * acos(
                cos(radians(:lat)) * cos(radians(i.latitude))
                * cos(radians(i.longitude) - radians(:lng))
                + sin(radians(:lat)) * sin(radians(i.latitude))
            )
          ) <= :radiusMeters
          AND i.id != :excludeId
        """, nativeQuery = true)
    List<Issue> findPotentialDuplicates(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radiusMeters") double radiusMeters,
        @Param("category") String category,
        @Param("excludeId") UUID excludeId
    );

    // ─── Heatmap data: lat/lng + count aggregation ────────────────────────────
    @Query(value = """
        SELECT
            ROUND(CAST(i.latitude AS numeric), 3) AS latitude,
            ROUND(CAST(i.longitude AS numeric), 3) AS longitude,
            COUNT(*) AS weight
        FROM issues i
        WHERE (:city IS NULL OR i.city = :city)
          AND (:category IS NULL OR i.category = :category)
          AND (:status IS NULL OR i.status = :status)
        GROUP BY ROUND(CAST(i.latitude AS numeric), 3), ROUND(CAST(i.longitude AS numeric), 3)
        """, nativeQuery = true)
    List<Object[]> getHeatmapData(
        @Param("city") String city,
        @Param("category") String category,
        @Param("status") String status
    );

    // ─── Paginated listing with filters ──────────────────────────────────────
    Page<Issue> findByCity(String city, Pageable pageable);
    Page<Issue> findByCityAndStatus(String city, IssueStatus status, Pageable pageable);
    Page<Issue> findByReporterId(UUID reporterId, Pageable pageable);
    Page<Issue> findByAssignedToId(UUID authorityId, Pageable pageable);
    Page<Issue> findByWard(String ward, Pageable pageable);

    // ─── SLA breach detection ─────────────────────────────────────────────────
    @Query("""
        SELECT i FROM Issue i
        WHERE i.status NOT IN ('RESOLVED', 'CLOSED', 'REJECTED')
          AND i.slaDeadline < :now
          AND i.slaBreach = false
        """)
    List<Issue> findSlaBreachedIssues(@Param("now") OffsetDateTime now);

    // ─── Analytics counts ─────────────────────────────────────────────────────
    long countByStatus(IssueStatus status);
    long countByStatusAndCity(IssueStatus status, String city);
    long countByCategoryAndStatus(IssueCategory category, IssueStatus status);

    @Modifying
    @Query("UPDATE Issue i SET i.upvoteCount = i.upvoteCount + 1 WHERE i.id = :id")
    void incrementUpvoteCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Issue i SET i.upvoteCount = i.upvoteCount - 1 WHERE i.id = :id AND i.upvoteCount > 0")
    void decrementUpvoteCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Issue i SET i.commentCount = i.commentCount + 1 WHERE i.id = :id")
    void incrementCommentCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Issue i SET i.slaBreach = true WHERE i.id = :id")
    void markSlaBreach(@Param("id") UUID id);

    // ─── Analytics Queries ───────────────────────────────────────────────────
    @Query("SELECT i.category, COUNT(i) FROM Issue i GROUP BY i.category")
    List<Object[]> getCategoryBreakdown();

    @Query("SELECT i.status, COUNT(i) FROM Issue i GROUP BY i.status")
    List<Object[]> getStatusCounts();

    @Query("SELECT i.department, COUNT(i), SUM(CASE WHEN i.slaBreach = true THEN 1 ELSE 0 END) FROM Issue i WHERE i.department IS NOT NULL GROUP BY i.department")
    List<Object[]> getSlaComplianceByDepartment();

    @Query(value = "SELECT CAST(i.created_at AS date) AS day, COUNT(*) FROM issues i GROUP BY day ORDER BY day ASC", nativeQuery = true)
    List<Object[]> getIssueTrends();

    @Query(value = """
        SELECT
            i.department,
            COUNT(i.id) as total_resolved,
            AVG(EXTRACT(EPOCH FROM (i.resolved_at - i.created_at)) / 3600) as avg_resolution_hours
        FROM issues i
        WHERE i.status IN ('RESOLVED', 'CLOSED')
          AND i.department IS NOT NULL
        GROUP BY i.department
        """, nativeQuery = true)
    List<Object[]> getDepartmentKpis();
}
