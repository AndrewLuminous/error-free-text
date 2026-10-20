package ru.andrew.errorfreetext.task;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "correction_task")
public class CorrectionTask {
    @Id
    private UUID id;
    @Column(nullable = false, columnDefinition = "text")
    private String originalText;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Language language;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TaskStatus status;
    @Column(columnDefinition = "text")
    private String correctedText;
    private Integer errorCode;
    @Column(columnDefinition = "text")
    private String errorMessage;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;

    protected CorrectionTask() {
    }

    public CorrectionTask(String text, Language language) {
        this.id = UUID.randomUUID();
        this.originalText = text;
        this.language = language;
        this.status = TaskStatus.NEW;
        this.createdAt = Instant.now();
    }

    public void start() {
        requireStatus(TaskStatus.NEW);
        status = TaskStatus.PROCESSING;
        startedAt = Instant.now();
    }

    public void complete(String text) {
        requireStatus(TaskStatus.PROCESSING);
        correctedText = text;
        status = TaskStatus.DONE;
        finishedAt = Instant.now();
    }

    public void fail(int code, String message) {
        requireStatus(TaskStatus.PROCESSING);
        errorCode = code;
        errorMessage = message;
        status = TaskStatus.FAILED;
        finishedAt = Instant.now();
    }

    private void requireStatus(TaskStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Недопустимый переход из статуса " + status);
        }
    }

    public UUID getId() { return id; }
    public String getOriginalText() { return originalText; }
    public Language getLanguage() { return language; }
    public TaskStatus getStatus() { return status; }
    public String getCorrectedText() { return correctedText; }
    public Integer getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
