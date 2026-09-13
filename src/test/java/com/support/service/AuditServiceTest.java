package com.support.service;

import com.support.entity.AuditLog;
import com.support.entity.LoginHistory;
import com.support.repository.AuditLogRepository;
import com.support.repository.LoginHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    @DisplayName("recordEntityChange — Persists AuditLog with correct entity, action, and user")
    void testRecordEntityChange() {
        auditService.recordEntityChange("TICKET", 100L, "CREATE", "customer1", "Ticket created");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getEntityName()).isEqualTo("TICKET");
        assertThat(saved.getEntityId()).isEqualTo(100L);
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getPerformedBy()).isEqualTo("customer1");
        assertThat(saved.getDetails()).isEqualTo("Ticket created");
    }

    @Test
    @DisplayName("recordLoginSuccess — Persists LoginHistory with SUCCESS and client IP/UserAgent")
    void testRecordLoginSuccess() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.50");
        request.addHeader("User-Agent", "Mozilla/5.0 TestBrowser");

        auditService.recordLoginSuccess("john_doe", request);

        ArgumentCaptor<LoginHistory> captor = ArgumentCaptor.forClass(LoginHistory.class);
        verify(loginHistoryRepository, times(1)).save(captor.capture());

        LoginHistory saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("john_doe");
        assertThat(saved.getStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.50");
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0 TestBrowser");
        assertThat(saved.getLogoutTime()).isNull();
    }

    @Test
    @DisplayName("recordLoginFailure — Persists LoginHistory with failure reason")
    void testRecordLoginFailure() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        auditService.recordLoginFailure("bad_user", request, "BAD_CREDENTIALS");

        ArgumentCaptor<LoginHistory> captor = ArgumentCaptor.forClass(LoginHistory.class);
        verify(loginHistoryRepository, times(1)).save(captor.capture());

        LoginHistory saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("bad_user");
        assertThat(saved.getStatus()).isEqualTo("FAILED: BAD_CREDENTIALS");
        assertThat(saved.getIpAddress()).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("recordLogout — Finds active session and sets logoutTime")
    void testRecordLogout() {
        LoginHistory activeSession = LoginHistory.builder()
                .id(5L)
                .username("john_doe")
                .status("SUCCESS")
                .loginTime(LocalDateTime.now().minusHours(1))
                .logoutTime(null)
                .build();

        when(loginHistoryRepository.findFirstByUsernameAndLogoutTimeIsNullOrderByLoginTimeDesc("john_doe"))
                .thenReturn(Optional.of(activeSession));

        auditService.recordLogout("john_doe");

        verify(loginHistoryRepository, times(1)).save(activeSession);
        assertThat(activeSession.getLogoutTime()).isNotNull();
    }
}
