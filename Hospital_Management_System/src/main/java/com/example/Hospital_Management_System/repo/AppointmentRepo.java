package com.example.Hospital_Management_System.repo;

import com.example.Hospital_Management_System.constant.APPOINTMENT_STATUS;
import com.example.Hospital_Management_System.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.jmx.export.naming.IdentityNamingStrategy;

import java.util.List;

public interface AppointmentRepo extends JpaRepository<Appointment, Integer>, PagingAndSortingRepository<Appointment,Integer> {
    @Query(value = "select * from appointment where doctor_id= :id",nativeQuery = true)
    public List<Appointment> findBYDoctorId(@Param("id") int id);

    @Query(value = "select * from appointment where patient_id= :id",nativeQuery = true)
    public List<Appointment> findBYPatientId(@Param("id") int id);
    List<Appointment> findByStatus(APPOINTMENT_STATUS status);
}
