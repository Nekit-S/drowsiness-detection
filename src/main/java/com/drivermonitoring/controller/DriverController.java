// What is this file?
// This controller handles the Driver screen: login form, monitoring start, and session exit.
// Why is this needed?
// It manages the frontend views and connects driver actions to the backend services.

package com.drivermonitoring.controller;

// Add imports for Driver and DriverRepository
import com.drivermonitoring.model.Driver;
import com.drivermonitoring.repository.DriverRepository;
// Import Logger
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import java.util.Optional; // Import Optional

@Controller
public class DriverController {

    // Initialize Logger
    private static final Logger logger = LoggerFactory.getLogger(DriverController.class);

    // Inject DriverRepository
    @Autowired
    private DriverRepository driverRepository;

    @GetMapping("/driver/login")
    public String driverLogin() {
        // Returns the view name for the driver login page
        return "driver_login";
    }

    @PostMapping("/driver/start")
    public String startMonitoring(@RequestParam String driverName,
                                   @RequestParam String driverId,
                                   HttpSession session,
                                   Model model) {
        // Validate driverId format
        if (driverId == null || !driverId.matches("\\d{6}")) {
             model.addAttribute("error", "Driver ID must be exactly 6 digits.");
             logger.warn("Invalid Driver ID format received: {}", driverId);
             return "driver_login";
        }

        logger.info("Attempting login for driver ID: {}", driverId);
        Optional<Driver> existingDriverOpt = driverRepository.findById(driverId);

        Driver driverToUse;

        if (existingDriverOpt.isPresent()) {
            // Driver with this ID exists, check the name
            Driver existingDriver = existingDriverOpt.get();
            logger.info("Driver found with ID: {}. Checking name.", driverId);
            if (!existingDriver.getDriverName().equalsIgnoreCase(driverName)) {
                // Name does not match
                logger.warn("Login attempt for ID: {} failed. Name mismatch. Provided: '{}', Expected: '{}'", 
                            driverId, driverName, existingDriver.getDriverName());
                model.addAttribute("error", "Driver ID exists but the name does not match.");
                // Keep entered values in the form for user convenience
                model.addAttribute("driverNameValue", driverName);
                model.addAttribute("driverIdValue", driverId);
                return "driver_login";
            } else {
                // Name matches, use the existing driver
                logger.info("Name matches for driver ID: {}. Proceeding with existing driver.", driverId);
                driverToUse = existingDriver;
            }
        } else {
            // Driver with this ID does not exist, create a new one
            logger.info("Driver with ID: {} not found. Creating new driver.", driverId);
            Driver newDriver = new Driver(driverId, driverName);
            try {
                driverToUse = driverRepository.save(newDriver);
                logger.info("Successfully saved new driver with ID: {}", driverToUse.getDriverId());
            } catch (Exception e) {
                logger.error("Error saving new driver with ID: {}", driverId, e);
                model.addAttribute("error", "Could not save new driver information. Please try again or contact support.");
                // Keep entered values
                model.addAttribute("driverNameValue", driverName);
                model.addAttribute("driverIdValue", driverId);
                return "driver_login";
            }
        }

        // Proceed with the session and redirect
        logger.info("Storing session attributes for driver ID: {}", driverToUse.getDriverId());
        session.setAttribute("driverName", driverToUse.getDriverName());
        session.setAttribute("driverId", driverToUse.getDriverId());

        model.addAttribute("driverName", driverToUse.getDriverName());
        model.addAttribute("driverId", driverToUse.getDriverId());

        logger.info("Redirecting to driver monitoring page for driver ID: {}", driverToUse.getDriverId());
        return "driver_monitoring";
    }

    @GetMapping("/driver/exit")
    public String exitSession(HttpSession session) {
        // Invalidate the session to log the driver out
        session.invalidate();
        // Redirect back to the login page
        return "redirect:/driver/login";
    }
}
