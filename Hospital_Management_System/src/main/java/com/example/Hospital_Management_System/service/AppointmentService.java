package com.example.Hospital_Management_System.service;

import com.example.Hospital_Management_System.constant.APPOINTMENT_STATUS;
import com.example.Hospital_Management_System.dto.AppointmentDTO;
import com.example.Hospital_Management_System.exception.ResourceNotFoundException;
import com.example.Hospital_Management_System.model.Appointment;
import com.example.Hospital_Management_System.repo.AppointmentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentService {
    private final AppointmentRepo appointmentRepo;
    @Autowired
    public AppointmentService(AppointmentRepo appointmentRepo){this.appointmentRepo= appointmentRepo;}
    public Appointment save(Appointment appointment) {return appointmentRepo.save(appointment);}
    public List<Appointment> findByDocId(int id){return appointmentRepo.findBYDoctorId(id);}
    public List<Appointment> findByPatientId(int id){ return appointmentRepo.findBYPatientId(id);}
    public List<Appointment> findAll(){ return appointmentRepo.findAll();}
    public Optional<Appointment> findById(int id){return appointmentRepo.findById(id); }

    public void updateAppointmentStatusToCompleted(int appointmentId){
        Appointment appointment= appointmentRepo.findById(appointmentId)
                .orElseThrow(()->
                        new ResourceNotFoundException("Appointment doesnot exist with id "+ appointmentId
        ));
        appointment.setStatus(APPOINTMENT_STATUS.COMPLETED);
        appointment.setId(appointmentId); // Not necessary because when we fetch the appointment the id is
       // already assigned
        save(appointment);
    }
    public void updateAppointmentStatusToSchelduded(int appointmentId){
        Appointment appointment= appointmentRepo.findById(appointmentId)
                .orElseThrow(()->
                        new ResourceNotFoundException("Appointment doesnot exist with id "+ appointmentId
                        ));
        appointment.setStatus(APPOINTMENT_STATUS.SCHEDULDED);
        save(appointment);
    }

    public void updateAppointmentStatusToCancled(int appointmentId) {
        Appointment appointment= appointmentRepo.findById(appointmentId)
                .orElseThrow(()->
                        new ResourceNotFoundException("Appointment doesnot exist with id "+ appointmentId
                        ));
        appointment.setStatus(APPOINTMENT_STATUS.CANCLED);
        save(appointment);
    }
    @Scheduled(cron = "0 0  0 * * *")//Check at midnight every day
    public void cancelAppointmentAfter10Days(){
        List<Appointment> appointmentList= findAll()
                .stream().filter((appointment)-> {
                           return appointment.getStatus() == APPOINTMENT_STATUS.PENDING ||
                                    appointment.getStatus() == APPOINTMENT_STATUS.SCHEDULDED;
                        } ).collect(Collectors.toList());
        for(Appointment appointment: appointmentList){
            long days= ChronoUnit.DAYS.between(appointment.getDate(), LocalDate.now());
            if(days>10){
                updateAppointmentStatusToCancled(appointment.getId());
            }
        }
    }

    public List<AppointmentDTO> findAllForAPatient(int id,String status) {
        try {
            APPOINTMENT_STATUS appointment_Status = APPOINTMENT_STATUS.valueOf(status.trim().toUpperCase());
       return appointmentRepo.findByStatus(appointment_Status).
                stream()
               .filter(appointment -> {return  appointment.getPatient()!=null;})
               .filter(appointment -> {return  appointment.getDoctor()!=null;})
               .filter(appointment -> {return appointment.getDate()!=null;})
               .filter(appointment -> {return appointment.getTime()!=null;})
               .filter(appointment -> {return appointment.getDate()!=null;})
               .filter(appointment -> {return appointment.getPatient().getId()==id;})

               .map(appointment -> {
                   AppointmentDTO appointmentDTO = new AppointmentDTO();
                   appointmentDTO.setAppointmentId(appointment.getId());
                   appointmentDTO.setPatientId(appointment.getPatient().getId());
                   appointmentDTO.setReason(appointment.getReason());
                   appointmentDTO.setLocalTime(appointment.getTime());
                   appointmentDTO.setAppointmentStatus(appointment.getStatus());
                  appointmentDTO.setDoctorId(appointment.getDoctor().getId());
                  appointmentDTO.setLocalDate(appointment.getDate());
                  return appointmentDTO;
               }).toList();
        }catch (Exception ex){
            throw new IllegalArgumentException("Invalid status provided: The valid appointment status are:" +
                    " PENDING,SCHEDULDED,COMPLETED,CANCLED");
        }
    }
}
