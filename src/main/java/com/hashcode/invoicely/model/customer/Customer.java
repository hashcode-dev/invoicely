package com.hashcode.invoicely.model.customer;

import com.hashcode.invoicely.model.common.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document("customers")
public class Customer {
    @Id
    private String id;                // Mongo _id

    @Indexed(unique = true)
    private String customerId;        // Generated like CUST-000001

    @Indexed
    private String name;

    @Indexed(unique = true)
    private String email;

    @Indexed(unique = true, sparse = true)
    private String primaryContact;

    private String secondaryContact;

    private Address address;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
