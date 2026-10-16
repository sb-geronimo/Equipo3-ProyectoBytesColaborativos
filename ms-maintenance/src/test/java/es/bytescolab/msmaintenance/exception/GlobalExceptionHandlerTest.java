package es.bytescolab.msmaintenance.exception;

import es.bytescolab.msmaintenance.config.SecurityConfig;
import es.bytescolab.msmaintenance.security.InternalKeyFilter;
import es.bytescolab.msmaintenance.security.JwtAuthFilter;
import es.bytescolab.msmaintenance.security.JwtUtil;
import es.bytescolab.msmaintenance.security.TokenAccessDeniedHandler;
import es.bytescolab.msmaintenance.security.TokenAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProbeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@org.springframework.test.context.ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private InternalKeyFilter internalKeyFilter;

    @MockitoBean
    private TokenAuthenticationEntryPoint tokenAuthenticationEntryPoint;

    @MockitoBean
    private TokenAccessDeniedHandler tokenAccessDeniedHandler;

    @RestController
    static class ProbeController {

        @GetMapping("/probe/from")
        public String getFrom(@RequestParam("from") LocalDate from) {
            return "ok";
        }

        @GetMapping("/probe/from-to")
        public String getFromTo(@RequestParam("from") LocalDate from,
                                @RequestParam("to") LocalDate to) {
            return "ok";
        }
    }

    @Test
    void missingRequiredParam_returns400ValidationError() throws Exception {
        mockMvc.perform(get("/probe/from-to"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("La petición contiene parámetros obligatorios ausentes"))
                .andExpect(jsonPath("$.details[0].field").value("from"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void invalidDateFormat_returns400ValidationError() throws Exception {
        mockMvc.perform(get("/probe/from").param("from", "no-es-fecha"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("La petición contiene parámetros con formato inválido"))
                .andExpect(jsonPath("$.details[0].field").value("from"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}