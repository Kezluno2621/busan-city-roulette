package com.cityroulette;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CityRouletteApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void homeRendersIndexTemplate() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("title", "City Roulette"))
                .andExpect(content().string(containsString("<h1>City Roulette</h1>")))
                .andExpect(content().string(containsString("여행 추천 기능은 아직 구현되지 않았습니다.")));
    }
}
