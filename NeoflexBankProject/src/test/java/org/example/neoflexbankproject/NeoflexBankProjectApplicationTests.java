package org.example.neoflexbankproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.neoflexbankproject.dto.ScoringDataDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.time.LocalDate;

@AutoConfigureMockMvc
@SpringBootTest
public class NeoflexBankProjectApplicationTests {
    @Autowired
    private MockMvc mockMvc;
}