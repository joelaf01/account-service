package com.jfessler.accountservice.representation;

import com.jfessler.accountservice.model.Status;

public record AccountRequest(String name, Status status) {}
