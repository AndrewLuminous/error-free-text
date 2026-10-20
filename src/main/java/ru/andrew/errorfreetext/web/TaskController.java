package ru.andrew.errorfreetext.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.andrew.errorfreetext.task.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CreatedTask> create(@Valid @RequestBody CreateTask request) {
        UUID id = service.create(request.text(), request.language());
        return ResponseEntity.accepted().location(URI.create("/api/tasks/" + id)).body(new CreatedTask(id));
    }

    @GetMapping("/{id}")
    public TaskResult get(@PathVariable UUID id) {
        CorrectionTask task = service.get(id);
        return new TaskResult(task.getStatus(), task.getCorrectedText(),
                task.getErrorCode(), task.getErrorMessage());
    }

    public record CreateTask(@NotBlank String text, @NotNull Language language) {}
    public record CreatedTask(UUID id) {}
    public record TaskResult(TaskStatus status, String correctedText, Integer errorCode, String errorMessage) {}
}
