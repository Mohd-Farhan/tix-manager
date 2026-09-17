package com.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for changing user passwords.
 * Separated from UserDTO to avoid accidental exposure or modification
 * of sensitive identity fields during password updates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordChangeDTO {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@#$!%*?&])[A-Za-z\\d@#$!%*?&]{8,}$",
            message = "Password must be at least 8 characters with 1 uppercase, 1 digit, and 1 special character (@#$!%*?&)"
    )
    private String newPassword;
}
