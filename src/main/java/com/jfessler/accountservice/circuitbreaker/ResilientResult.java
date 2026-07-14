package com.jfessler.accountservice.circuitbreaker;

public record ResilientResult<T>(T value, boolean stale) {}
