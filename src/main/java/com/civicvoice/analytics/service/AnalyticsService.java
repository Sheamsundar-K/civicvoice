package com.civicvoice.analytics.service;

import com.civicvoice.issue.repository.IssueRepository;
import com.civicvoice.poll.repository.PollRepository;
import com.civicvoice.poll.repository.PollVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final IssueRepository issueRepository;
    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;

    public Map<String, Object> getDashboardStats() {
        List<Object[]> rawCounts = issueRepository.getStatusCounts();
        Map<String, Long> statusCounts = new HashMap<>();
        long totalIssues = 0;

        for (Object[] row : rawCounts) {
            String status = String.valueOf(row[0]);
            long count = ((Number) row[1]).longValue();
            statusCounts.put(status, count);
            totalIssues += count;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIssues", totalIssues);
        stats.put("byStatus", statusCounts);
        stats.put("totalPolls", pollRepository.countTotalPolls());
        stats.put("totalVotes", pollVoteRepository.count());
        return stats;
    }

    public List<Map<String, Object>> getCategoryBreakdown() {
        List<Object[]> raw = issueRepository.getCategoryBreakdown();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> map = new HashMap<>();
            map.put("category", String.valueOf(row[0]));
            map.put("count", ((Number) row[1]).longValue());
            result.add(map);
        }
        return result;
    }

    public List<Map<String, Object>> getSlaCompliance() {
        List<Object[]> raw = issueRepository.getSlaComplianceByDepartment();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> map = new HashMap<>();
            String dept = String.valueOf(row[0]);
            long total = ((Number) row[1]).longValue();
            long breached = ((Number) row[2]).longValue();
            long compliant = total - breached;
            double complianceRate = total == 0 ? 100.0 : ((double) compliant / total) * 100.0;

            map.put("department", dept);
            map.put("totalIssues", total);
            map.put("breachedCount", breached);
            map.put("compliantCount", compliant);
            map.put("complianceRate", Math.round(complianceRate * 100.0) / 100.0);
            result.add(map);
        }
        return result;
    }

    public List<Map<String, Object>> getTrends() {
        List<Object[]> raw = issueRepository.getIssueTrends();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> map = new HashMap<>();
            map.put("date", String.valueOf(row[0]));
            map.put("count", ((Number) row[1]).longValue());
            result.add(map);
        }
        return result;
    }

    public List<Map<String, Object>> getDepartmentKpis() {
        List<Object[]> raw = issueRepository.getDepartmentKpis();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> map = new HashMap<>();
            map.put("department", String.valueOf(row[0]));
            map.put("resolvedCount", ((Number) row[1]).longValue());
            double avgHours = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            map.put("avgResolutionHours", Math.round(avgHours * 10.0) / 10.0);
            result.add(map);
        }
        return result;
    }

    public Map<String, Object> getPollEngagement() {
        long totalPolls = pollRepository.countTotalPolls();
        long totalVotes = pollVoteRepository.count();
        double avgVotes = totalPolls == 0 ? 0.0 : (double) totalVotes / totalPolls;

        Map<String, Object> result = new HashMap<>();
        result.put("totalPolls", totalPolls);
        result.put("totalVotes", totalVotes);
        result.put("avgVotesPerPoll", Math.round(avgVotes * 10.0) / 10.0);
        return result;
    }
}
