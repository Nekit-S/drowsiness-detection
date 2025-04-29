// What is this file?
// This controller handles the Driver screen: login form, monitoring start, and session exit.
// Why is this needed?
// It manages the frontend views and connects driver actions to the backend services.

package com.drivermonitoring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

@Controller
public class DriverController {

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
        // Validate driverId format (e.g., 6 digits) - Basic validation
        if (driverId == null || !driverId.matches("\\d{6}")) {
             model.addAttribute("error", "Driver ID must be exactly 6 digits.");
             return "driver_login"; // Return to login page with error
        }

        // Store driver info in the session
        session.setAttribute("driverName", driverName);
        session.setAttribute("driverId", driverId);

        // Add driver name to the model for display on the monitoring page
        model.addAttribute("driverName", driverName);
        model.addAttribute("driverId", driverId); // Also pass driverId

        // Returns the view name for the driver monitoring page
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
