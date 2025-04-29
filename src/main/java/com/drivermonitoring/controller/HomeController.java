// Что это за файл?
// Этот класс обслуживает базовую HTTP конечную точку для проверки работы бэкенда.
// Зачем это нужно?
// Это необходимо для подтверждения того, что проект собирается и работает правильно перед добавлением более сложной логики.

package com.drivermonitoring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Hello World from Driver Monitoring System";
    }
}