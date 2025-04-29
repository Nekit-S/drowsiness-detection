// What is this file?
// This class represents a Driver entity stored in the database.
// Why is this needed?
// It stores the driver's ID and name, which are used to associate driving sessions.

package com.drivermonitoring.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "drivers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    private String driverId; // Must be exactly 6 digits
    
    private String driverName;
}