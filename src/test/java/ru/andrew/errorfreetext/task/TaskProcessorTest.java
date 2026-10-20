package ru.andrew.errorfreetext.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.andrew.errorfreetext.speller.*;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaskProcessorTest {
    private TaskService tasks;
    private SpellerClient client;
    private TaskProcessor processor;

    @BeforeEach
    void setUp() {
        tasks = mock(TaskService.class);
        client = mock(SpellerClient.class);
        processor = new TaskProcessor(tasks, client, new TextChunker(), new TextOptions(), new CorrectionApplier());
    }

    @Test
    void savesCorrectedResult() {
        CorrectionTask task = task("Превет, мир!");
        when(client.check(task.getOriginalText(), Language.RU, 0))
                .thenReturn(List.of(new SpellingError(0, 6, "Превет", List.of("Привет"))));
        processor.processNext();
        verify(tasks).complete(task.getId(), "Привет, мир!");
        verify(tasks, never()).fail(any(), anyInt(), anyString());
    }

    @Test
    void stopsAfterFirstFailedFragmentWithoutRetry() {
        CorrectionTask task = task("Текст ".repeat(5000));
        when(client.check(anyString(), eq(Language.RU), eq(0)))
                .thenReturn(List.of()).thenThrow(new SpellerException(50401, "Тайм-аут"));
        processor.processNext();
        verify(client, times(2)).check(anyString(), eq(Language.RU), eq(0));
        verify(tasks).fail(task.getId(), 50401, "Тайм-аут");
        verify(tasks, never()).complete(any(), anyString());
    }

    @Test
    void calculatesOptionsFromWholeText() {
        CorrectionTask task = task("Текст ".repeat(2000) + "https://example.org/123");
        when(client.check(anyString(), eq(Language.RU), eq(6))).thenReturn(List.of());
        processor.processNext();
        verify(client, atLeast(2)).check(anyString(), eq(Language.RU), eq(6));
        verify(tasks).complete(task.getId(), task.getOriginalText());
    }

    @Test
    void doesNotCallProviderForEmptyQueue() {
        when(tasks.claimNext()).thenReturn(Optional.empty());
        processor.processNext();
        verifyNoInteractions(client);
    }

    private CorrectionTask task(String text) {
        CorrectionTask task = new CorrectionTask(text, Language.RU);
        task.start();
        when(tasks.claimNext()).thenReturn(Optional.of(task));
        return task;
    }
}
