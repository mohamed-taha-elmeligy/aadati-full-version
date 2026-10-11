package com.mts.aadati.engine.core.job;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class JobRegistry {

    private final List<Job> orderedJobs;

    public JobRegistry(List<Job> jobs) {
        this.orderedJobs = sort(jobs);
    }

    public List<Job> ordered() {
        return orderedJobs;
    }

    private List<Job> sort(List<Job> jobs) {
        Map<String, Job> byId = indexById(jobs);

        Map<Job, Integer> inDegree = new LinkedHashMap<>();
        Map<Job, List<Job>> dependents = new LinkedHashMap<>();
        buildGraph(jobs, byId, inDegree, dependents);

        List<Job> ordered = topologicalOrder(inDegree, dependents);

        if (ordered.size() != jobs.size())
            throw new IllegalStateException("Circular dependency detected among: " + unresolvedNames(jobs, ordered));

        return List.copyOf(ordered);
    }

    private String unresolvedNames(List<Job> jobs, List<Job> ordered) {
        Set<Job> unresolved = new LinkedHashSet<>(jobs);
        ordered.forEach(unresolved::remove);
        return unresolved.stream()
                .map(Job::id)
                .collect(Collectors.joining(", "));
    }

    private Map<String, Job> indexById(List<Job> jobs) {
        Map<String, Job> byId = new LinkedHashMap<>();
        for (Job job : jobs) {
            if (byId.put(job.id(), job) != null)
                throw new IllegalStateException("Duplicate job id: " + job.id());
        }
        return byId;
    }

    private void buildGraph(List<Job> jobs,
                            Map<String, Job> byId,
                            Map<Job, Integer> inDegree,
                            Map<Job, List<Job>> dependents) {
        for (Job job : jobs) {
            inDegree.put(job, distinctDependencies(job).size());
            dependents.put(job, new ArrayList<>());
        }
        for (Job job : jobs) {
            for (String dep : distinctDependencies(job)) {
                dependents.get(resolve(job, dep, byId)).add(job);
            }
        }
    }

    private List<String> distinctDependencies(Job job) {
        return job.dependsOn().stream().distinct().toList();
    }

    private Job resolve(Job job, String dep, Map<String, Job> byId) {
        Job depJob = byId.get(dep);
        if (depJob == null)
            throw new IllegalStateException(job.id() + " depends on missing job: " + dep);
        return depJob;
    }

    private List<Job> topologicalOrder(Map<Job, Integer> inDegree,
                                       Map<Job, List<Job>> dependents) {
        Deque<Job> queue = inDegree.entrySet().stream()
                .filter(e -> e.getValue() == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayDeque::new));

        List<Job> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            Job current = queue.poll();
            result.add(current);
            releaseDependents(current, inDegree, dependents, queue);
        }
        return result;
    }

    private void releaseDependents(Job current,
                                   Map<Job, Integer> inDegree,
                                   Map<Job, List<Job>> dependents,
                                   Deque<Job> queue) {
        for (Job next : dependents.get(current)) {
            if (inDegree.merge(next, -1, Integer::sum) == 0)
                queue.add(next);
        }
    }
}