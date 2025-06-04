package com.example.Hospital_Management_System.controller;

import com.example.Hospital_Management_System.constant.APPOINTMENT_STATUS;
import com.example.Hospital_Management_System.constant.USER_ROLE;
import com.example.Hospital_Management_System.dto.AppointmentDTO;
import com.example.Hospital_Management_System.dto.AppointmentSaveDTO;
import com.example.Hospital_Management_System.exception.ResourceNotFoundException;
import com.example.Hospital_Management_System.model.Appointment;
import com.example.Hospital_Management_System.model.Doctor;
import com.example.Hospital_Management_System.model.Patient;
import com.example.Hospital_Management_System.model.User;
import com.example.Hospital_Management_System.repo.DoctorRepo;
import com.example.Hospital_Management_System.repo.PatientRepo;
import com.example.Hospital_Management_System.service.AppointmentService;
import com.example.Hospital_Management_System.service.GrantAccess;
import com.example.Hospital_Management_System.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/appointment")
@CrossOrigin(origins = "*") // Only for local development!
@Tag(
        name = "Appointment APIs",
        description = "APIs for managing appointments including creation, " +
                "filtering by doctor/patient/status, and retrieval. Accessible by ADMIN, " +
                "DOCTOR, and PATIENT roles where applicable."
)
public class AppointmentController {
    private final AppointmentService appointmentService;

    @Autowired
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @Autowired
    private UserService userService;

    @Autowired
    private DoctorRepo doctorRepo;
    @Autowired
    private PatientRepo patientRepo;
    @Autowired
    private GrantAccess grantAccess;

    //access: adimin only
    @Operation(
            summary = "Create a new appointment",
            description = "Allows ADMIN to create a new appointment by specifying the doctor and patient IDs, reason, date, and time.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful Operation"),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid ID or parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource"),
                    @ApiResponse(responseCode = "404", description = "Not Found - No resource found with given ID"),
                    @ApiResponse(responseCode = "409", description = "Conflict - Resource already exists or violates constraints"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @PostMapping("/create")
    public ResponseEntity<AppointmentDTO> createAppointment(@Valid @RequestBody AppointmentSaveDTO appointmentSaveDTO) throws AccessDeniedException {
        Patient patient = patientRepo.findById(appointmentSaveDTO.getPatientId()).orElseThrow(() ->
                new ResourceNotFoundException("Patient Not Found with Id " + appointmentSaveDTO.getPatientId()));
        Doctor doctor = doctorRepo.findById(appointmentSaveDTO.getDoctorId()).orElseThrow(() ->
                new ResourceNotFoundException("Doctor Not Found with Id" + appointmentSaveDTO.getDoctorId()));
        Appointment appointment= new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setReason(appointmentSaveDTO.getReason());
        appointment.setTime(appointmentSaveDTO.getTime());
        appointment.setDate(appointmentSaveDTO.getDate());
        appointmentService.save(appointment);
        return ResponseEntity.ok(createAppointmentDTO(appointment));
    }

    //access: That specific doctor and admin
    @Operation(
            summary = "Get appointments by doctor ID",
            description = "Returns all appointments for a specific doctor. Accessible by ADMIN and the doctor themselves.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful Operation"),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid ID or parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource"),
                    @ApiResponse(responseCode = "404", description = "Not Found - No resource found with given ID"),
                    @ApiResponse(responseCode = "409", description = "Conflict - Resource already exists or violates constraints"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @GetMapping("/find-by-doctorid/{docId}")
    public ResponseEntity<List<AppointmentDTO>> findBydocId(@PathVariable int docId) throws AccessDeniedException {
        User user = grantAccess.getAuthenticationUser();
        boolean isDoctor = user.getDoctor() != null;
        boolean isAdmin = user.getRole() == USER_ROLE.ADMIN;
        if (isAdmin || (isDoctor && user.getDoctor().getId() == docId)) {

            List<AppointmentDTO> appointmentDTOList = appointmentService.findByDocId(docId)
                    .stream()
                    .map((appointment) ->
                            createAppointmentDTO(appointment)
                    ).collect(toList());
            return ResponseEntity.ok(appointmentDTOList);
        }
        throw new AccessDeniedException("Sorry, Access denied");
    }
    //Admin and that specific doctor
    @Operation(
            summary = "Get appointments by doctor ID and status",
            description = "Returns all appointments for a specific doctor filtered by appointment status (e.g PENDING,SCHEDULDED,COMPLETED,CANCLED). Accessible by ADMIN and the respective doctor.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful Operation"),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid ID or parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource"),
                    @ApiResponse(responseCode = "404", description = "Not Found - No resource found with given ID"),
                    @ApiResponse(responseCode = "409", description = "Conflict - Resource already exists or violates constraints"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")}
    )
   @GetMapping("/find-by-docid-and-status")
   public ResponseEntity<List<AppointmentDTO>>findBydoctIdByStatus(@RequestParam(required = true) int docId,
                               @RequestParam(required = true) String status)throws  AccessDeniedException{
       // Convert the status string to the enum
       APPOINTMENT_STATUS appointmentStatus;
       try {
           appointmentStatus = APPOINTMENT_STATUS.valueOf(status.toUpperCase()); // convert to enum
       } catch (IllegalArgumentException e) {
           throw new IllegalArgumentException("Invalid status value provided.");
       }
        boolean isAdmin= grantAccess.getAuthenticationUser().getRole()==USER_ROLE.ADMIN;
        boolean isDoctor= grantAccess.getAuthenticationUser().getDoctor()!=null;
        if(isAdmin ||(isDoctor && docId== grantAccess.getAuthenticationUser().getDoctor().getId())) {
            List<AppointmentDTO> appointmentDTOList = appointmentService.findAll()
                    .stream()
                    .filter((appointment) ->
                            (appointment.getDoctor()!=null && appointment.getPatient()!=null) &&
                            ( appointment.getDoctor().getId() == docId &&
                                    appointment.getStatus()==appointmentStatus))
                    .map((appointment) -> {
                        return createAppointmentDTO(appointment);
                    })
                    .toList();
            return ResponseEntity.ok(appointmentDTOList);
        }
        throw new AccessDeniedException("Sorry, Access Denied");
   }


    // access: that specific doctor, that specific patient and admin all
    @Operation(
            summary = "Get appointments by doctor and patient ID",
            description = "Returns all appointments between a specific doctor and patient. Accessible by ADMIN, the doctor involved, and the patient involved.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful Operation"),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid ID or parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource"),
                    @ApiResponse(responseCode = "404", description = "Not Found - No resource found with given ID"),
                    @ApiResponse(responseCode = "409", description = "Conflict - Resource already exists or violates constraints"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")}
    )
        @GetMapping("/find")
        public ResponseEntity<List<AppointmentDTO>> findByDocAndPatientId ( @RequestParam int doctorId, @RequestParam
        int patientId)throws AccessDeniedException {
            User user = grantAccess.getAuthenticationUser();
            Doctor doctor = null;
            Patient patient = null;
            boolean isDoctor = user.getDoctor() != null;
            boolean isPatient = user.getPatient() != null;
            boolean isAdmin = user.getRole() == USER_ROLE.ADMIN;

            if (isAdmin || (isDoctor && user.getDoctor().getId() == doctorId) ||
                    (isPatient && user.getPatient().getId()== patientId)) {

                List<AppointmentDTO> appointmentDTOList = findBydocId(doctorId).getBody().stream().filter((appointmentDTO) ->
                        appointmentDTO.getPatientId() == patientId
                ).toList();

                return ResponseEntity.ok(appointmentDTOList);
            }
            throw new AccessDeniedException("Sorry, Access denied");
        }
        // access: admin only
        @Operation(
                summary = "Get all appointments",
                description = "Returns a complete list of all appointments in the system. Accessible by ADMIN only.",
                security = @SecurityRequirement(name = "bearerAuth"),
                responses = {
                        @ApiResponse(responseCode = "200", description = "Successful Operation"),
                        @ApiResponse(responseCode = "400", description = "Bad Request - Invalid ID or parameters"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource"),
                        @ApiResponse(responseCode = "404", description = "Not Found - No resource found with given ID"),
                        @ApiResponse(responseCode = "409", description = "Conflict - Resource already exists or violates constraints"),
                        @ApiResponse(responseCode = "500", description = "Internal Server Error")}
        )
        @GetMapping("/findall")
        public ResponseEntity<List<AppointmentDTO>> findAll ()throws AccessDeniedException {
            List<AppointmentDTO> appointmentDTOList = appointmentService.findAll()
                    .stream().map((appointment) -> {
                                return createAppointmentDTO(appointment);
                            }
                    ).toList();
            return ResponseEntity.ok(appointmentDTOList);
        }

        private AppointmentDTO createAppointmentDTO (Appointment appointment){
            AppointmentDTO appointmentDTO = new AppointmentDTO();
            appointmentDTO.setAppointmentId(appointment.getId());
            if (appointment.getDoctor() != null) {
                appointmentDTO.setDoctorId(appointment.getDoctor().getId());
            }
            if (appointment.getPatient() != null) {

                appointmentDTO.setPatientId(appointment.getPatient().getId());
            }
            appointmentDTO.setLocalDate(appointment.getDate());
            appointmentDTO.setLocalTime(appointment.getTime());
            appointmentDTO.setReason(appointment.getReason());
            appointmentDTO.setAppointmentStatus(appointment.getStatus());
            return appointmentDTO;
        }

    @Operation(
            summary = "Get all appointments",
            description = "Returns a complete list of all appointments of a specific patient. Accessible by Patient only",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful Operation"),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid ID or parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - You do not have permission to access this resource"),
                    @ApiResponse(responseCode = "404", description = "Not Found - No resource found with given ID"),
                    @ApiResponse(responseCode = "409", description = "Conflict - Resource already exists or violates constraints"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")}
    )
    @GetMapping("/find_all_appointments_of_mine")
        public ResponseEntity<List<AppointmentDTO>> findALlForAPatient(@RequestParam(required = true) String status){
        int patientId= grantAccess.getAuthenticationUser().getPatient().getId();
        return ResponseEntity.ok(appointmentService.findAllForAPatient(patientId,status));
        }
    }

