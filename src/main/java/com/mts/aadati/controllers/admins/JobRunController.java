package com.mts.aadati.controllers.admins;

import com.mts.aadati.dto.response.PageModel;
import com.mts.aadati.engine.core.domain.JobRun;
import com.mts.aadati.engine.core.domain.JobRunStore;
import com.mts.aadati.utils.uris.Uri;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Uri.BASE + Uri.ADMIN + Uri.JOB)
@RequiredArgsConstructor
public class JobRunController {

    private final JobRunStore jobRunStore;

    @GetMapping("/runs")
    public ResponseEntity<PageModel<JobRun>> findRuns(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(required = false) String jobId
    ) {
        Page<JobRun> runs = (jobId == null)
                ? jobRunStore.findRuns(pageNumber)
                : jobRunStore.findRunsByJobId(jobId, pageNumber);
        return ResponseEntity.ok(PageModel.from(runs));
    }

    @GetMapping("/{jobId}/last")
    public ResponseEntity<JobRun> lastRun(@PathVariable String jobId) {
        return jobRunStore.lastRun(jobId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{jobId}/last-success")
    public ResponseEntity<JobRun> lastSuccess(@PathVariable String jobId) {
        return jobRunStore.lastSuccess(jobId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
