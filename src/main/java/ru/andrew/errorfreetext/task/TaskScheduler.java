package ru.andrew.errorfreetext.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskScheduler {
    private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);
    private final TaskProcessor processor;
    private final TaskService tasks;
    private volatile boolean ready;

    public TaskScheduler(TaskProcessor processor, TaskService tasks) {
        this.processor = processor;
        this.tasks = tasks;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        // В проекте запускается один экземпляр приложения. Прерванные задачи не повторяем.
        int recovered = tasks.recoverInterrupted();
        log.info("При запуске завершено прерванных задач: {}", recovered);
        ready = true;
    }

    @Scheduled(fixedDelayString = "${app.scheduler-delay}")
    public void tick() {
        if (ready) {
            processor.processNext();
        }
    }
}
