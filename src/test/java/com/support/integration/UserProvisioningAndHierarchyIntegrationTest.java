package com.support.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.dto.BulkUploadResultDTO;
import com.support.dto.CreateUserRequest;
import com.support.dto.LoginDTO;
import com.support.dto.UserDTO;
import com.support.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ==============================================================================================
 * INTEGRATION TEST SUITE: System Admin Role, Enterprise IAM & Bulk Provisioning
 * ==============================================================================================
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserProvisioningAndHierarchyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginDTO loginDto = new LoginDTO();
        loginDto.setUsername(username);
        loginDto.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test
    @DisplayName("Scenario 1: Public registration endpoint must be disabled and inaccessible")
    void testPublicRegistration_Disabled() throws Exception {
        UserDTO registration = UserDTO.builder()
                .username("random_person")
                .email("random@example.com")
                .password("Secret123!")
                .role(UserRole.CUSTOMER)
                .build();

        // /api/auth/register is no longer permitAll or mapped -> must return 403 Forbidden or 404 Not Found
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 2: System Admin provisions an Admin; Admin can provision Agent but cannot escalate to Admin")
    void testRoleHierarchyAndPrivilegeEscalationSafeguards() throws Exception {
        // Step 1: System Admin logs in with seeded credentials
        String sysAdminToken = loginAndGetToken("sysadmin", "sysadmin123");
        assertThat(sysAdminToken).isNotBlank();

        // Step 2: System Admin creates an Admin account
        CreateUserRequest adminCreation = CreateUserRequest.builder()
                .username("regional_admin")
                .email("regional_admin@tixmanager.com")
                .password("RegionalAdmin@123")
                .role(UserRole.ADMIN)
                .build();

        MvcResult adminCreateResult = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + sysAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminCreation)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("regional_admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        Long regionalAdminId = objectMapper.readTree(adminCreateResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Step 3: Newly created Admin logs in
        String regionalAdminToken = loginAndGetToken("regional_admin", "RegionalAdmin@123");
        assertThat(regionalAdminToken).isNotBlank();

        // Step 4: Admin attempts privilege escalation by trying to create another ADMIN -> MUST FAIL (400)
        CreateUserRequest rogueAdmin = CreateUserRequest.builder()
                .username("rogue_admin")
                .email("rogue@test.com")
                .password("RoguePass123!")
                .role(UserRole.ADMIN)
                .build();

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + regionalAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rogueAdmin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Admins cannot create Admin or System Admin accounts."));

        // Step 5: Admin attempts privilege escalation by trying to create a SYSTEM_ADMIN -> MUST FAIL (400)
        CreateUserRequest rogueSysAdmin = CreateUserRequest.builder()
                .username("rogue_sysadmin")
                .email("rogue_sys@test.com")
                .password("RoguePass123!")
                .role(UserRole.SYSTEM_ADMIN)
                .build();

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + regionalAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rogueSysAdmin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Admins cannot create Admin or System Admin accounts."));

        // Step 6: Admin creates a valid SUPPORT_AGENT -> MUST SUCCEED (201)
        CreateUserRequest validAgent = CreateUserRequest.builder()
                .username("triage_agent_1")
                .email("triage1@tixmanager.com")
                .password("AgentSecret123!")
                .role(UserRole.SUPPORT_AGENT)
                .build();

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + regionalAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAgent)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("triage_agent_1"))
                .andExpect(jsonPath("$.role").value("SUPPORT_AGENT"));

        // Step 7: Admin attempts to deactivate themselves -> MUST FAIL (400)
        mockMvc.perform(delete("/api/users/" + regionalAdminId)
                        .header("Authorization", "Bearer " + regionalAdminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot deactivate your own account."));

        // Step 8: Admin attempts to deactivate the seeded System Admin -> MUST FAIL (400)
        // seeded sysadmin id is typically 1
        mockMvc.perform(delete("/api/users/1")
                        .header("Authorization", "Bearer " + regionalAdminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Admins cannot deactivate Admin or System Admin accounts."));

        // Step 9: System Admin deactivates regional_admin -> MUST SUCCEED (204)
        mockMvc.perform(delete("/api/users/" + regionalAdminId)
                        .header("Authorization", "Bearer " + sysAdminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Scenario 3: Bulk CSV upload with partial error reporting & default password assignment")
    void testBulkUpload_PartialSuccessAndDefaultPassword() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");

        // CSV containing:
        // Row 1: Valid Agent with explicit password
        // Row 2: Valid Customer with blank password (should get default password: bulk_cust_2@123)
        // Row 3: Admin account attempt by regular admin -> must be flagged as error
        // Row 4: Malformed email -> must be flagged as error
        String csvContent = "username,email,password,role\n" +
                "bulk_agent_1,bulk_agent1@tixmanager.com,AgentPass@123,SUPPORT_AGENT\n" +
                "bulk_cust_2,bulk_cust2@example.com,,CUSTOMER\n" +
                "bulk_admin_attempt,admin_fail@test.com,,ADMIN\n" +
                "bulk_bad_email,invalid-email,,CUSTOMER\n";

        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "users.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/users/bulk-upload")
                        .file(csvFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        BulkUploadResultDTO result = objectMapper.readValue(
                uploadResult.getResponse().getContentAsString(),
                BulkUploadResultDTO.class
        );

        assertThat(result.getTotalRows()).isEqualTo(4);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(2);
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors().get(0)).contains("Admins cannot create ADMIN accounts");
        assertThat(result.getErrors().get(1)).contains("Invalid email format");

        // Verify that bulk_cust_2 was created with the default password: bulk_cust_2@123
        String customerToken = loginAndGetToken("bulk_cust_2", "bulk_cust_2@123");
        assertThat(customerToken).isNotBlank();

        // Verify bulk_agent_1 can log in with explicit password
        String agentToken = loginAndGetToken("bulk_agent_1", "AgentPass@123");
        assertThat(agentToken).isNotBlank();
    }
}
