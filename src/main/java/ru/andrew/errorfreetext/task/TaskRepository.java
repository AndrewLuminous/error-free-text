package ru.andrew.errorfreetext.task;

import org.springframework.data.jpa.repository.*;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<CorrectionTask, UUID> {
    @Query(value = """
        SELECT * FROM correction_task WHERE status = 'NEW'
        ORDER BY created_at, id LIMIT 1 FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<CorrectionTask> findNextForProcessing();

    @Modifying
    @Query(value = """
        UPDATE correction_task
        SET status = 'FAILED', error_code = 50002,
            error_message = 'Обработка прервана перезапуском приложения',
            finished_at = CURRENT_TIMESTAMP
        WHERE status = 'PROCESSING'
        """, nativeQuery = true)
    int failInterruptedTasks();
}
