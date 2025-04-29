// What is this file?
// This controller handles the Driver screen: login form, monitoring start, and session exit.
// Why is this needed?
// It manages the frontend views and connects driver actions to the backend services.

package com.drivermonitoring.controller;

import com.drivermonitoring.model.Driver; // Add import
import com.drivermonitoring.model.DriverSession;
import com.drivermonitoring.repository.DriverRepository; // Add import
import com.drivermonitoring.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired; // Add import
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

@Controller
public class DriverController {

    @Autowired
    private SessionService sessionService;

    @Autowired // Inject DriverRepository
    private DriverRepository driverRepository;

    @GetMapping("/driver/login")
    public String driverLogin() {
        // Returns the view name for the driver login page
        return "driver_login";
    }

    @PostMapping("/driver/start")
    public String startMonitoring(@RequestParam String driverName,
                                   @RequestParam String driverId,
                                   HttpSession httpSession,
                                   Model model) {
        // Validate driverId format (e.g., 6 digits) - Basic validation
        if (driverId == null || !driverId.matches("\\d{6}")) {
             model.addAttribute("error", "Driver ID must be exactly 6 digits.");
             return "driver_login"; // Return to login page with error
        }

        // Save driver information if it doesn't exist
        if (!driverRepository.existsById(driverId)) {
            Driver driver = new Driver(driverId, driverName);
            driverRepository.save(driver);
        }

        // Start the session using the service
        DriverSession driverSession = sessionService.startSession(driverId);

        if (driverSession == null) {
            // Handle error if session couldn't be started (e.g., invalid driverId was somehow passed)
            model.addAttribute("error", "Could not start monitoring session.");
            return "driver_login";
        }

        // Store driver info and session ID in the HTTP session
        httpSession.setAttribute("driverName", driverName);
        httpSession.setAttribute("driverId", driverId);
        httpSession.setAttribute("sessionId", driverSession.getSessionId()); // Store session ID

        // Add data to the model for the monitoring page
        model.addAttribute("driverName", driverName);
        model.addAttribute("driverId", driverId);
        model.addAttribute("sessionId", driverSession.getSessionId()); // Pass session ID to the view

        // Returns the view name for the driver monitoring page
        return "driver_monitoring";
    }

    @GetMapping("/driver/exit")
    public String exitSession(HttpSession httpSession) { // Renamed to avoid conflict
        // Get driverId from the session before invalidating
        String driverId = (String) httpSession.getAttribute("driverId");

        if (driverId != null) {
            // End the session using the service
            sessionService.endSession(driverId);
        } else {
            // Log a warning if driverId wasn't found in session
            // Consider adding a logger instance to the class
            System.err.println("Warning: driverId not found in session during exit.");
        }

        // Invalidate the HTTP session to log the driver out
        httpSession.invalidate();
        // Redirect back to the login page
        return "redirect:/driver/login";
    }
}
