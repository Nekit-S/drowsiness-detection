// What is this file?
// Repository interface for accessing Driver data from the database.
// Why is this needed?
// It allows easy CRUD operations on Driver entities without boilerplate code.

package com.drivermonitoring.repository;

import com.drivermonitoring.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, String> {
}