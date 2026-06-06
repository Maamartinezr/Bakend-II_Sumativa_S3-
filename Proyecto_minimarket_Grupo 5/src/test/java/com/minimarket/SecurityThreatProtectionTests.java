package com.minimarket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityThreatProtectionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicResponsesIncludeSecurityHeadersToReduceBrowserAttackSurface() throws Exception {
        mockMvc.perform(get("/public/hola"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", containsString("camera=()")));
    }

    @Test
    void xssPayloadInJsonBodyIsBlockedBeforeAuthentication() throws Exception {
        String maliciousLogin = """
                {
                  "username": "<script>alert('xss')</script>",
                  "password": "Cliente123!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousLogin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La solicitud contiene patrones potencialmente maliciosos"));
    }

    @Test
    void xssPayloadInQueryStringIsBlocked() throws Exception {
        mockMvc.perform(get("/api/productos")
                        .param("busqueda", "<img src=x onerror=alert(1)>"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La solicitud contiene patrones potencialmente maliciosos"));
    }

    @Test
    void sqlInjectionPayloadInLoginIsBlockedAndDoesNotBypassAuthentication() throws Exception {
        String sqlInjectionLogin = """
                {
                  "username": "admin' OR '1'='1",
                  "password": "cualquierPassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sqlInjectionLogin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La solicitud contiene patrones potencialmente maliciosos"));
    }

    @Test
    void sqlInjectionPayloadInRequestParametersIsBlocked() throws Exception {
        mockMvc.perform(get("/api/productos")
                        .param("nombre", "' OR '1'='1 --"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La solicitud contiene patrones potencialmente maliciosos"));
    }

    @Test
    void csrfStyleMutationWithoutJwtIsRejectedEvenIfCsrfHeaderIsPresent() throws Exception {
        mockMvc.perform(post("/api/carrito")
                        .header("Origin", "http://localhost:3000")
                        .header("X-CSRF-TOKEN", "token-falso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightFromUntrustedOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/productos")
                        .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedAdminCanAccessUserManagementWithJwt() throws Exception {
        String token = loginAndGetToken("admin", "Admin123!");

        mockMvc.perform(get("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void weakPasswordIsRejectedByServerSideValidation() throws Exception {
        String token = loginAndGetToken("admin", "Admin123!");
        String weakUser = """
                {
                  "username": "nuevoadmin",
                  "password": "simple",
                  "roles": ["ADMIN"]
                }
                """;

        mockMvc.perform(post("/api/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(weakUser))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La password debe tener al menos 10 caracteres"));
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String login = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }
}
