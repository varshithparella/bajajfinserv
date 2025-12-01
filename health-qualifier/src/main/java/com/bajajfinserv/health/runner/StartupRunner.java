package com.bajajfinserv.health.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.bajajfinserv.health.model.SolutionRequest;
import com.bajajfinserv.health.model.WebhookRequest;
import com.bajajfinserv.health.model.WebhookResponse;

@Component
public class StartupRunner implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(StartupRunner.class);
    
    private static final String GENERATE_WEBHOOK_URL = 
        "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
    
    private static final String TEST_WEBHOOK_URL = 
        "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";
    
    private final RestTemplate restTemplate;
    
    public StartupRunner() {
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting Bajaj Finserv Health Qualifier application...");
        
        try {
            // Step 1: Generate webhook
            WebhookResponse webhookResponse = generateWebhook();
            logger.info("Webhook generated successfully");
            logger.info("Webhook URL: {}", webhookResponse.getWebhook());
            
            // Step 2: Prepare SQL solution (Question 1 - Odd regNo)
            String sqlQuery = getSqlSolution();
            logger.info("SQL Query prepared: {}", sqlQuery);
            
            // Step 3: Submit solution
            submitSolution(webhookResponse.getAccessToken(), sqlQuery);
            logger.info("Solution submitted successfully!");
            
        } catch (Exception e) {
            logger.error("Error during execution: ", e);
            throw e;
        }
    }
    
    private WebhookResponse generateWebhook() {
        WebhookRequest request = new WebhookRequest(
            "Varshith Parella",
            "22BCE8721",
            "parellavarshith@gmail.com"
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<WebhookRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<WebhookResponse> response = restTemplate.postForEntity(
            GENERATE_WEBHOOK_URL,
            entity,
            WebhookResponse.class
        );
        
        return response.getBody();
    }
    
    private String getSqlSolution() {
        return "SELECT " +
               "d.DEPARTMENT_NAME, " +
               "emp_salary.SALARY, " +
               "emp_salary.EMPLOYEE_NAME, " +
               "emp_salary.AGE " +
               "FROM DEPARTMENT d " +
               "INNER JOIN ( " +
               "    SELECT " +
               "        e.DEPARTMENT, " +
               "        SUM(p.AMOUNT) AS SALARY, " +
               "        CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS EMPLOYEE_NAME, " +
               "        TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, " +
               "        e.EMP_ID " +
               "    FROM EMPLOYEE e " +
               "    INNER JOIN PAYMENTS p ON e.EMP_ID = p.EMP_ID " +
               "    WHERE DAY(p.PAYMENT_TIME) != 1 " +
               "    GROUP BY e.EMP_ID, e.DEPARTMENT, e.FIRST_NAME, e.LAST_NAME, e.DOB " +
               ") emp_salary ON d.DEPARTMENT_ID = emp_salary.DEPARTMENT " +
               "INNER JOIN ( " +
               "    SELECT " +
               "        e.DEPARTMENT, " +
               "        MAX(total_salary) AS max_salary " +
               "    FROM ( " +
               "        SELECT " +
               "            e.EMP_ID, " +
               "            e.DEPARTMENT, " +
               "            SUM(p.AMOUNT) AS total_salary " +
               "        FROM EMPLOYEE e " +
               "        INNER JOIN PAYMENTS p ON e.EMP_ID = p.EMP_ID " +
               "        WHERE DAY(p.PAYMENT_TIME) != 1 " +
               "        GROUP BY e.EMP_ID, e.DEPARTMENT " +
               "    ) e " +
               "    GROUP BY e.DEPARTMENT " +
               ") max_sal ON emp_salary.DEPARTMENT = max_sal.DEPARTMENT " +
               "    AND emp_salary.SALARY = max_sal.max_salary " +
               "ORDER BY d.DEPARTMENT_NAME";
    }
    
    private void submitSolution(String accessToken, String sqlQuery) {
        SolutionRequest request = new SolutionRequest(sqlQuery);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", accessToken);
        
        HttpEntity<SolutionRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            TEST_WEBHOOK_URL,
            entity,
            String.class
        );
        
        logger.info("Response status: {}", response.getStatusCode());
        logger.info("Response body: {}", response.getBody());
    }
}

