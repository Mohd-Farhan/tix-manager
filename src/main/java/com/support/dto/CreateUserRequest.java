package com.support.dto;

import com.support.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for administrative user creation")
public class CreateUserRequest {

    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Unique user login name", example = "john_doe")
    private String username;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email format must be valid")
    @Schema(description = "Unique email address", example = "john@example.com")
    private String email;

    /**
     * Optional initial password. If blank, defaults to '<username>@123'.
     * When provided, must satisfy complexity: min 8 chars, 1 uppercase, 1 digit, 1 special char.
     */
    @Pattern(
            regexp = "^$|^(?=.*[A-Z])(?=.*\\d)(?=.*[@#$!%*?&])[A-Za-z\\d@#$!%*?&]{8,}$",
            message = "Password must be at least 8 characters with 1 uppercase, 1 digit, and 1 special character (@#$!%*?&)"
    )
    @Schema(description = "Initial password. If left blank, defaults to '<username>@123'", example = "SecurePass1!")
    private String password;

    @NotNull(message = "Role must be specified")
    @Schema(description = "Assigned user role", example = "CUSTOMER")
    private UserRole role;
}
