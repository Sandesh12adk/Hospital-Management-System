package com.example.Hospital_Management_System.repo;

import com.example.Hospital_Management_System.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepo extends JpaRepository<Patient,Integer>, PagingAndSortingRepository<Patient,Integer> {
}
