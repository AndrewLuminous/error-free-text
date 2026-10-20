package ru.andrew.errorfreetext.task;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SchedulerContextTest {
    @Test
    void schedulerStartsWithSpringBootAutoConfiguration() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
                .withUserConfiguration(SchedulingConfiguration.class)
                .withPropertyValues("app.scheduler-delay=3000")
                .withBean(TaskService.class, () -> mock(TaskService.class))
                .withBean(TaskProcessor.class, () -> mock(TaskProcessor.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(CorrectionTaskScheduler.class);
                    assertThat(context).hasSingleBean(org.springframework.scheduling.TaskScheduler.class);
                    verifyNoInteractions(context.getBean(TaskProcessor.class));
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @Import(CorrectionTaskScheduler.class)
    static class SchedulingConfiguration {}
}
