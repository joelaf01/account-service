package com.jfessler.accountservice.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@Table(schema = "accounts")
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Account {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Status status;
}
