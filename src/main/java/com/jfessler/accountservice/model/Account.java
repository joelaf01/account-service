package com.jfessler.accountservice.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@Table(schema = "accounts")
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    private UUID id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Status status;
}
