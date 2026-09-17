package com.support.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.dto.CreateMessageRequest;
import com.support.dto.CreateTicketRequest;
import com.support.dto.CreateUserRequest;
import com.support.dto.LoginDTO;
import com.support.dto.TicketResponse;
import com.support.entity.TicketPriority;
import com.support.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ==============================================================================================
 * VERIFICATION & END-TO-END SIGN-OFF TEST: Full Lifecycle Journey
 * ==============================================================================================
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TixManagerE2EVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Phase 1 E2E Sign-Off: Complete Customer & Agent Ticket Lifecycle")
    void testCompleteTicketLifecycle_E2E() throws Exception {

        // --------------------------------------------------------------------------------------
        // STEP 1: Admin Provisions New Customer Account via POST /api/users
        // --------------------------------------------------------------------------------------
        LoginDTO adminLogin = new LoginDTO();
        adminLogin.setUsername("admin");
        adminLogin.setPassword("admin123");

        MvcResult adminLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String adminJwt = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString())
                .get("token").asText();

        CreateUserRequest customerCreation = CreateUserRequest.builder()
                .username("e2e_customer")
                .email("e2e_customer@test.com")
                .password("Password123!")
                .role(UserRole.CUSTOMER)
                .build();

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerCreation)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("e2e_customer"))
                .andExpect(jsonPath("$.email").value("e2e_customer@test.com"));


        // --------------------------------------------------------------------------------------
        // STEP 2: Customer Logs In & Obtains JWT Bearer Token
        // --------------------------------------------------------------------------------------
        LoginDTO customerLogin = new LoginDTO();
        customerLogin.setUsername("e2e_customer");
        customerLogin.setPassword("Password123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode customerAuthJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String customerJwt = customerAuthJson.get("token").asText();
        Long customerId = customerAuthJson.get("user").get("id").asLong();
        assertThat(customerJwt).isNotBlank();
        assertThat(customerId).isPositive();

        // --------------------------------------------------------------------------------------
        // STEP 3: Customer Files a New Ticket using Bearer Token
        // --------------------------------------------------------------------------------------
        CreateTicketRequest newTicket = CreateTicketRequest.builder()
                .title("Production API returning HTTP 504 gateway timeout")
                .description("All external webhook callbacks are timing out after 30 seconds.")
                .priority(TicketPriority.HIGH)
                .build();

        MvcResult ticketResult = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTicket)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andReturn();

        TicketResponse createdTicket = objectMapper.readValue(ticketResult.getResponse().getContentAsString(), TicketResponse.class);
        Long ticketId = createdTicket.getId();
        assertThat(ticketId).isNotNull();

        // --------------------------------------------------------------------------------------
        // STEP 4: Support Agent Logs In (Using seeded agent account 'priya_agent')
        // --------------------------------------------------------------------------------------
        LoginDTO agentLogin = new LoginDTO();
        agentLogin.setUsername("priya_agent");
        agentLogin.setPassword("agent123");

        MvcResult agentLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentLogin)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode agentAuthJson = objectMapper.readTree(agentLoginResult.getResponse().getContentAsString());
        String agentJwt = agentAuthJson.get("token").asText();
        Long agentId = agentAuthJson.get("user").get("id").asLong();

        // --------------------------------------------------------------------------------------
        // STEP 5: Agent Claims / Assigns Ticket -> Status transitions to IN_PROGRESS
        // --------------------------------------------------------------------------------------
        mockMvc.perform(put("/api/tickets/" + ticketId + "/assign?agentId=" + agentId)
                        .header("Authorization", "Bearer " + agentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignedAgentId").value(agentId));

        // --------------------------------------------------------------------------------------
        // STEP 6: Customer Sends Follow-up Message in Thread
        // --------------------------------------------------------------------------------------
        CreateMessageRequest customerMessage = CreateMessageRequest.builder()
                .content("Attaching traceroute logs for the 504 gateway timeout.")
                .build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/messages")
                        .header("Authorization", "Bearer " + customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerMessage)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Attaching traceroute logs for the 504 gateway timeout."));

        // --------------------------------------------------------------------------------------
        // STEP 7: Agent Replies to Customer
        // --------------------------------------------------------------------------------------
        CreateMessageRequest agentReply = CreateMessageRequest.builder()
                .content("Thanks for the logs. We isolated the issue to the gateway proxy and deployed a fix.")
                .build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/messages")
                        .header("Authorization", "Bearer " + agentJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentReply)))
                .andExpect(status().isCreated());

        // --------------------------------------------------------------------------------------
        // STEP 8: Agent Resolves Ticket
        // --------------------------------------------------------------------------------------
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status?status=RESOLVED")
                        .header("Authorization", "Bearer " + agentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // --------------------------------------------------------------------------------------
        // STEP 9: Verify Message Thread & Audit History Timeline
        // --------------------------------------------------------------------------------------
        // Verify Message Thread contains both messages
        mockMvc.perform(get("/api/tickets/" + ticketId + "/messages")
                        .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Verify Status History Timeline contains OPEN -> IN_PROGRESS -> RESOLVED entries
        mockMvc.perform(get("/api/tickets/" + ticketId + "/history")
                        .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3)) // 1: Initial Open, 2: In Progress, 3: Resolved
                .andExpect(jsonPath("$[0].newStatus").value("OPEN"))
                .andExpect(jsonPath("$[1].newStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[2].newStatus").value("RESOLVED"));
    }

    @Test
    @DisplayName("Bulk Upload & History Sign-Off: CSV upload records immutable audit history")
    void testBulkUploadAndHistory_E2E() throws Exception {
        LoginDTO adminLogin = new LoginDTO();
        adminLogin.setUsername("admin");
        adminLogin.setPassword("admin123");

        MvcResult adminLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String adminJwt = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString())
                .get("token").asText();

        // 1. Admin uploads CSV with 1 valid user and 1 invalid row
        String csvContent = "username,email,password,role\n" +
                "batch_user1,batch1@example.com,Pass123!,CUSTOMER\n" +
                "bad_row,not-an-email,,CUSTOMER\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "e2e_batch.csv", "text/csv", csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/users/bulk-upload")
                        .file(file)
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(1));

        // 2. Admin queries history
        mockMvc.perform(get("/api/users/bulk-upload/history")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("e2e_batch.csv"))
                .andExpect(jsonPath("$[0].uploadedBy").value("admin"))
                .andExpect(jsonPath("$[0].totalRows").value(2))
                .andExpect(jsonPath("$[0].successCount").value(1))
                .andExpect(jsonPath("$[0].failureCount").value(1))
                .andExpect(jsonPath("$[0].status").value("PARTIAL_SUCCESS"))
                .andExpect(jsonPath("$[0].errors.length()").value(1));
    }
}
