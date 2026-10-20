package ru.andrew.errorfreetext.task;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.andrew.errorfreetext.web.ApiException;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {
    private final TaskRepository repository;
    private final int maxTextLength;

    public TaskService(TaskRepository repository, @Value("${app.max-text-length}") int maxTextLength) {
        this.repository = repository;
        this.maxTextLength = maxTextLength;
    }

    @Transactional
    public UUID create(String text, Language language) {
        if (text == null || language == null || text.codePointCount(0, text.length()) < 3
                || text.length() > maxTextLength || text.codePoints().noneMatch(Character::isLetter)) {
            throw new ApiException(400, 40001,
                    "Нужны язык RU или EN и текст от 3 до " + maxTextLength + " символов, содержащий буквы");
        }
        return repository.save(new CorrectionTask(text, language)).getId();
    }

    @Transactional(readOnly = true)
    public CorrectionTask get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(404, 40401, "Задача не найдена"));
    }

    @Transactional
    public Optional<CorrectionTask> claimNext() {
        Optional<CorrectionTask> task = repository.findNextForProcessing();
        task.ifPresent(CorrectionTask::start);
        return task;
    }

    @Transactional
    public void complete(UUID id, String text) {
        get(id).complete(text);
    }

    @Transactional
    public void fail(UUID id, int code, String message) {
        get(id).fail(code, message);
    }

    @Transactional
    public int recoverInterrupted() {
        return repository.failInterruptedTasks();
    }
}
