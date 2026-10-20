package ru.andrew.errorfreetext.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.andrew.errorfreetext.speller.*;

@Service
public class TaskProcessor {
    private static final Logger log = LoggerFactory.getLogger(TaskProcessor.class);
    private final TaskService tasks;
    private final SpellerClient client;
    private final TextChunker chunker;
    private final TextOptions options;
    private final CorrectionApplier applier;

    public TaskProcessor(TaskService tasks, SpellerClient client, TextChunker chunker,
                         TextOptions options, CorrectionApplier applier) {
        this.tasks = tasks;
        this.client = client;
        this.chunker = chunker;
        this.options = options;
        this.applier = applier;
    }

    public void processNext() {
        tasks.claimNext().ifPresent(this::process);
    }

    private void process(CorrectionTask task) {
        log.info("Начата обработка задачи {}, длина={}", task.getId(), task.getOriginalText().length());
        String corrected;
        try {
            int flags = options.resolve(task.getOriginalText());
            StringBuilder result = new StringBuilder();
            // Сетевые вызовы выполняются после завершения транзакции захвата задачи.
            for (TextChunker.Chunk chunk : chunker.split(task.getOriginalText())) {
                result.append(chunk.check()
                        ? applier.apply(chunk.text(), client.check(chunk.text(), task.getLanguage(), flags))
                        : chunk.text());
            }
            corrected = result.toString();
        } catch (SpellerException ex) {
            tasks.fail(task.getId(), ex.getCode(), ex.getMessage());
            log.warn("Задача {} завершена с ошибкой {}", task.getId(), ex.getCode());
            return;
        } catch (RuntimeException ex) {
            tasks.fail(task.getId(), 50001, "Внутренняя ошибка обработки текста");
            log.error("Ошибка обработки задачи {}: {}", task.getId(), ex.getClass().getSimpleName());
            return;
        }
        tasks.complete(task.getId(), corrected);
        log.info("Задача {} выполнена", task.getId());
    }
}
