package ru.andrew.errorfreetext.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.andrew.errorfreetext.task.*;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskControllerTest {
    private TaskService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(TaskService.class);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mvc = MockMvcBuilders.standaloneSetup(new TaskController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build();
    }

    @Test
    void createsTaskWithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.create("Hello", Language.EN)).thenReturn(id);
        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hello\",\"language\":\"EN\"}"))
                .andExpect(status().isAccepted()).andExpect(header().string("Location", "/api/tasks/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void pendingResultContainsOnlyStatus() throws Exception {
        CorrectionTask task = new CorrectionTask("Hello", Language.EN);
        when(service.get(task.getId())).thenReturn(task);
        mvc.perform(get("/api/tasks/" + task.getId())).andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"NEW\"}", true));
    }

    @Test
    void finishedResultContainsTextOrError() throws Exception {
        CorrectionTask task = new CorrectionTask("Hello", Language.EN);
        task.start();
        task.fail(50401, "Тайм-аут");
        when(service.get(task.getId())).thenReturn(task);
        mvc.perform(get("/api/tasks/" + task.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorCode").value(50401))
                .andExpect(jsonPath("$.correctedText").doesNotExist());
    }

    @Test
    void validatesLanguageAndReturnsCommonErrorFormat() throws Exception {
        mvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hello\",\"language\":\"DE\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value(40001))
                .andExpect(jsonPath("$.errorMessage").isString())
                .andExpect(jsonPath("$.timestamp").exists()).andExpect(jsonPath("$.path").value("/api/tasks"));
        verifyNoInteractions(service);
    }
}
