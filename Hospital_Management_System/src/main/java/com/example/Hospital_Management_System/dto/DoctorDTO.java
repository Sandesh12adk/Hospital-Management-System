package com.example.Hospital_Management_System.dto;

import lombok.Data;

@Data
public class DoctorDTO {
    private String name;
    private String email;
    private String role;
    private String specialization;
    private String deparementName;
    private  String description;
    private int userId;
    private int doctorId;
    private int departmentId;
}
