package io.github.shaojunying.onlineexecutor.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Full application context with the default (process) mode.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExecutionControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void runsCode() {
        assertThat(post("""
                {"source": "public class A { public static void main(String[] a) { System.out.print(6 * 7); } }"}
                """))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.status").isEqualTo("SUCCESS");
                    json.assertThat().extractingPath("$.stdout").isEqualTo("42");
                    json.assertThat().extractingPath("$.exitCode").isEqualTo(0);
                });
    }

    @Test
    void passesStdin() {
        assertThat(post("""
                {"source": "public class A { public static void main(String[] a) throws Exception { System.out.print(new String(System.in.readAllBytes())); } }",
                 "stdin": "hi"}
                """))
                .hasStatusOk()
                .bodyJson().extractingPath("$.stdout").isEqualTo("hi");
    }

    @Test
    void returnsCompileErrorsAsNormalResult() {
        assertThat(post("""
                {"source": "public class A {\\n  int x = ;\\n}"}
                """))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    json.assertThat().extractingPath("$.status").isEqualTo("COMPILE_ERROR");
                    json.assertThat().extractingPath("$.compileErrors[0].line").isEqualTo(2);
                });
    }

    @Test
    void rejectsEmptySourceWithProblemDetail() {
        assertThat(post("""
                {"source": ""}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.title").isEqualTo("Invalid request");
    }

    @Test
    void describesRuntime() {
        assertThat(mvc.get().uri("/api/runtime"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.mode").isEqualTo("PROCESS");
    }

    @Test
    void servesPage() {
        assertThat(mvc.get().uri("/index.html"))
                .hasStatusOk()
                .bodyText().contains("<title>OnlineExecutor</title>");
    }

    @Test
    void redirectsOldPageUrl() {
        assertThat(mvc.get().uri("/executor"))
                .hasStatus(HttpStatus.MOVED_PERMANENTLY)
                .hasRedirectedUrl("/");
    }

    @Test
    void exposesHealth() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("UP");
    }

    private MockMvcTester.MockMvcRequestBuilder post(String json) {
        return mvc.post().uri("/api/executions").contentType(MediaType.APPLICATION_JSON).content(json);
    }
}
