// What is this file?
// This enum defines all possible states of a driver.
// Why is this needed?
// It provides a standardized way to represent driver states across the application.

package com.drivermonitoring.model;

public enum DriverState {
    NORMAL,     // Driver is alert and focused
    DISTRACTED, // Driver is not looking at the road
    DROWSY      // Driver appears to be sleepy or fatigued
}