// What is this file?
// This controller manages the Dispatcher panel: list drivers, select a driver, view stats and logs.
// Why is this needed?
// It connects the database (Driver, Event entities) with the Dispatcher front-end.

package com.drivermonitoring.controller;

import com.drivermonitoring.model.Driver;
import com.drivermonitoring.model.Event;
import com.drivermonitoring.repository.DriverRepository;
import com.drivermonitoring.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class DispatcherController {

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private EventRepository eventRepository;

    @GetMapping("/dispatcher")
    public String dispatcherPanel(Model model) {
        List<Driver> drivers = driverRepository.findAll();
        model.addAttribute("drivers", drivers);
        return "dispatcher_panel";
    }

    @GetMapping("/dispatcher/driver/{driverId}")
    public String driverStats(@PathVariable String driverId, Model model) {
        Driver driver = driverRepository.findById(driverId).orElse(null);
        // Use the existing method that orders by time
        List<Event> events = eventRepository.findByDriverIdOrderByStartTimeDesc(driverId);

        model.addAttribute("driver", driver);
        model.addAttribute("events", events);
        // Pass drivers list again for potential navigation back or header display
        List<Driver> drivers = driverRepository.findAll();
        model.addAttribute("drivers", drivers);
        return "driver_statistics"; // Ensure this template name matches the file
    }
}
