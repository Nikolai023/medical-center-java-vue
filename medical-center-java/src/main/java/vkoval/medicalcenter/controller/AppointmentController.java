package vkoval.medicalcenter.controller;

import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import vkoval.medicalcenter.controller.models.AppointmentInfo;
import vkoval.medicalcenter.controller.models.OrderInfo;
import vkoval.medicalcenter.dao.AppointmentRepository;
import vkoval.medicalcenter.dao.MedicalServiceRepository;
import vkoval.medicalcenter.dao.UserRepository;
import vkoval.medicalcenter.entity.schedule.Appointment;
import vkoval.medicalcenter.entity.service.MedicalService;
import vkoval.medicalcenter.entity.user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class AppointmentController {
    private MedicalServiceRepository serviceRepository;
    private AppointmentRepository appointmentRepository;
    private UserRepository userRepository;

    public AppointmentController(MedicalServiceRepository serviceRepository, AppointmentRepository appointmentRepository, UserRepository userRepository) {
        this.serviceRepository = serviceRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/appointments/all")
    public List<AppointmentInfo> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(AppointmentInfo::fromAppointment)
                .collect(Collectors.toList());
    }

    @GetMapping("/appointments/getByService")
    public List<AppointmentInfo> getAppointmentsByService(@RequestParam(value = "service_id") Long serviceId) {
        if (!serviceRepository.existsById(serviceId)) {
            return Collections.emptyList();
        }

        return appointmentRepository.findAllByMedicalServiceId(serviceId)
                .stream()
                .map(AppointmentInfo::fromAppointment)
                .collect(Collectors.toList());
    }

    @GetMapping("/appointments/getByDateAndServiceAvailable")
    public List<AppointmentInfo> getAvailableAppointmentsByDay(@RequestParam(value = "service_id") Long serviceId,
                                                               @RequestParam Integer year,
                                                               @RequestParam Integer month,
                                                               @RequestParam Integer day) {
        LocalDate date = LocalDate.of(year, month, day);

        return appointmentRepository.findByDayAndMedicalServiceIdAAndReserverIsNull(date, serviceId).stream()
                .map(AppointmentInfo::fromAppointment)
                .collect(Collectors.toList());
    }

    @PostMapping("/appointments/add")
    public void addAppointment(
            @RequestParam(value = "service_id") Long serviceId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam Integer day,
            @RequestParam String time
    ) {
        if (serviceId == null || year == null || month == null || day == null || StringUtils.isEmpty(time)) {
            return;
        }
        Optional<MedicalService> medicalService = serviceRepository.findById(serviceId);
        if (medicalService.isEmpty()) {
            return;
        }

        LocalDateTime appointmentTime = LocalDate.of(year, month, day).atTime(LocalTime.parse(time));

        Appointment appointment = new Appointment();
        appointment.setTime(appointmentTime.atZone(ZoneId.systemDefault()).toInstant());
        appointment.setMedicalService(medicalService.get());

        appointmentRepository.save(appointment);
    }

    @GetMapping("/appointments/order/{id}")
    public void orderAppointment(Authentication authentication, @PathVariable Long id) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }
        User currentUser = userRepository.findByLogin((String) authentication.getPrincipal());
        Optional<Appointment> appointment = appointmentRepository.findById(id);

        appointment.ifPresent(a -> {
            a.setReserver(currentUser);
            appointmentRepository.save(a);
        });

    }

    @GetMapping("/appointments/getOrders")
    public Iterable<OrderInfo> getUserOrders(Authentication authentication) {
        if (!authentication.isAuthenticated()) {
            return Collections.emptyList();
        }

        User currentUser = userRepository.findByLogin((String) authentication.getPrincipal());
        if (currentUser == null) {
            return Collections.emptyList();
        }

        return appointmentRepository.findAllByReserverId(currentUser.getId())
                .stream()
                .map(OrderInfo::fromAppointment)
                .sorted(Comparator.comparing(OrderInfo::getRecord))
                .collect(Collectors.toList());
    }

    @GetMapping("/appointments/monthAvailability")
    public Collection<Integer> getUserOrders(@RequestParam(value = "service_id") Long serviceId,
                                             @RequestParam Integer year,
                                             @RequestParam Integer month) {
        if (serviceId == null || year == null || month == null) {
            return Collections.emptyList();
        }
        Optional<MedicalService> medicalService = serviceRepository.findById(serviceId);
        if (medicalService.isEmpty()) {
            return Collections.emptyList();
        }

        int monthLength = LocalDate.of(year, month, 1).lengthOfMonth();
        Set<Integer> days = new HashSet<>();
        for (int dayNum = 1; dayNum <= monthLength; dayNum++) {
            LocalDate day = LocalDate.of(year, month, dayNum);
            boolean dayAvailable =
                    !appointmentRepository.findByDayAndMedicalServiceIdAAndReserverIsNull(day, serviceId).isEmpty();
            if (dayAvailable) {
                days.add(dayNum);
            }
        }
        return days;
    }
}
