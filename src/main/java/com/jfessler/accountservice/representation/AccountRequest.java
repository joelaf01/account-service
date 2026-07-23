package com.jfessler.accountservice.representation;

import com.jfessler.accountservice.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountRequest(@NotBlank @Size(max = 255) String name,
                             @NotNull Status status) {}
