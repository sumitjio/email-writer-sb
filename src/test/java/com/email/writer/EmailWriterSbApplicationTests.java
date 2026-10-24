package com.email.writer;

import com.email.writer.app.EmailGeneratorService;
import com.email.writer.app.EmailRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class EmailWriterSbApplicationTests {

	@Autowired
	private EmailGeneratorService emailGeneratorService;

	@Test
	void contextLoads() {
	}

	@Test
	void testGenerateEmailReply() {
		EmailRequest request = new EmailRequest();
		request.setEmailContent("Hi, are we still meeting today at 3 PM?");
		request.setTone("professional");

		String reply = emailGeneratorService.generateEmailReply(request);
		assertNotNull(reply);
		assertFalse(reply.isBlank());
		assertFalse(reply.startsWith("Error"));
		System.out.println("Generated Reply: " + reply);
	}

}

