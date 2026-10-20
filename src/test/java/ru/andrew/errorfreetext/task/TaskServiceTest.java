package ru.andrew.errorfreetext.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.andrew.errorfreetext.web.ApiException;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskServiceTest {
    private TaskRepository repository;
    private TaskService service;

    @BeforeEach
    void setUp() {
        repository = mock(TaskRepository.class);
        service = new TaskService(repository, 100_000);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ab", "123456", "!!!", "  ", "😀😀😀"})
    void rejectsInvalidText(String text) {
        assertThatThrownBy(() -> service.create(text, Language.RU)).isInstanceOf(ApiException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void acceptsLongTextAndCreatesNewTask() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.create("Текст ".repeat(3000), Language.RU)).isNotNull();
        verify(repository).save(argThat(task -> task.getStatus() == TaskStatus.NEW
                && task.getOriginalText().length() == 18000 && task.getFinishedAt() == null));
    }

    @Test
    void rejectsTooLongTextAndMissingLanguage() {
        assertThatThrownBy(() -> service.create("a".repeat(100001), Language.EN)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.create("abc", null)).isInstanceOf(ApiException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void returns404ForMissingTask() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id)).isInstanceOfSatisfying(ApiException.class,
                ex -> assertThat(ex.getStatus()).isEqualTo(404));
    }

    @Test
    void claimedTaskHasProcessingStatusAndStartTime() {
        CorrectionTask task = new CorrectionTask("Привет", Language.RU);
        when(repository.findNextForProcessing()).thenReturn(Optional.of(task));
        assertThat(service.claimNext()).contains(task);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PROCESSING);
        assertThat(task.getStartedAt()).isNotNull();
    }

    @Test
    void cannotCompleteNewOrAlreadyCompletedTask() {
        CorrectionTask task = new CorrectionTask("Hello", Language.EN);
        assertThatThrownBy(() -> task.complete("Hello")).isInstanceOf(IllegalStateException.class);
        task.start();
        task.complete("Hello");
        assertThat(task.getFinishedAt()).isNotNull();
        assertThatThrownBy(() -> task.fail(50001, "Ошибка")).isInstanceOf(IllegalStateException.class);
    }
}
