package com.cst438.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.cst438.domain.FinalGradeDTO;
import com.cst438.domain.Course;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentDTO;
import com.cst438.domain.EnrollmentRepository;

@Service
@ConditionalOnProperty(prefix = "registration", name = "service", havingValue = "rest")
@RestController
public class RegistrationServiceREST implements RegistrationService {
	
	
	RestTemplate restTemplate = new RestTemplate();
	
	@Value("${registration.url}") 
	String registration_url;
	
	public RegistrationServiceREST() {
		System.out.println("REST registration service ");
	}
	
	@Override
	public void sendFinalGrades(int course_id , FinalGradeDTO[] grades) { 
		
		ResponseEntity<FinalGradeDTO> response = restTemplate.postForEntity(
							"/course/{course_id}/finalgrades",
							grades,
							FinalGradeDTO.class);
		
		if (response.getStatusCodeValue() == 200) {
			// update database
			FinalGradeDTO finalGrades = response.getBody();
			restTemplate.put("http://localhost:8080/course", finalGrades);		
		} else {
			// error.
			System.out.println(
                         "Error: unable to post multiply_level "+
                          response.getStatusCodeValue());
		}
		
	}
	
	@Autowired
	CourseRepository courseRepository;

	@Autowired
	EnrollmentRepository enrollmentRepository;

	
	/*
	 * endpoint used by registration service to add an enrollment to an existing
	 * course.
	 */
	@PostMapping("/enrollment")
	@Transactional
	public EnrollmentDTO addEnrollment(@RequestBody EnrollmentDTO enrollmentDTO) {
		
		// Receive message from registration service to enroll a student into a course.
		
		System.out.println("GradeBook addEnrollment "+enrollmentDTO);
		
		EnrollmentDTO newEnrollment;
		String instructorEmail = "dwisneski@csumb.edu";  // user name (should be instructor's email)
		Course c = courseRepository.findById(enrollmentDTO.courseId()).orElse(null);
		if (c==null || ! c.getInstructor().equals(instructorEmail)) {
			throw  new ResponseStatusException( HttpStatus.BAD_REQUEST, "course id not found or not authorized "+ enrollmentDTO.courseId());
		}

		Enrollment e = new Enrollment();
		e.setStudentName(enrollmentDTO.studentName());
		e.setStudentEmail(enrollmentDTO.studentEmail());
		e.setCourse(c);
		enrollmentRepository.save(e);
		
		return enrollmentDTO;
		
	}

}
