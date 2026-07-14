package com.jfessler.accountservice.representation;

import com.jfessler.accountservice.model.Status;
import java.util.UUID;

public record AccountResponse(UUID id, String name, Status status, boolean stale) {}
