package com.loopers.interfaces.api.like;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.like.LikeFacade;
import com.loopers.interfaces.api.ApiControllerAdvice;
import com.loopers.interfaces.api.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LikeV1ControllerStandaloneTest {

    private MockMvc mockMvc;
    private LikeFacade likeFacade;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        likeFacade = mock(LikeFacade.class);
        LikeV1Controller controller = new LikeV1Controller(likeFacade);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ApiControllerAdvice())
            .build();
    }

    @DisplayName("좋아요 추가")
    @Nested
    class AddLike {

        @DisplayName("요청 본문이 없으면, 400을 반환한다.")
        @Test
        void returns400_whenBodyIsMissing() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/likes")
                    .header("X-Loopers-LoginId", "testuser")
                    .header("X-Loopers-LoginPw", "Test1234!")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }

        @DisplayName("productId가 null이면, 400을 반환한다.")
        @Test
        void returns400_whenProductIdIsNull() throws Exception {
            // Act
            String json = mockMvc.perform(post("/api/v1/likes")
                    .header("X-Loopers-LoginId", "testuser")
                    .header("X-Loopers-LoginPw", "Test1234!")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"productId": null}
                    """))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

            // Assert
            ApiResponse<?> response = objectMapper.readValue(json, new TypeReference<ApiResponse<?>>() {});
            assertThat(response.meta().result()).isEqualTo(ApiResponse.Metadata.Result.FAIL);
        }

        @DisplayName("유효한 요청이면, 200을 반환한다.")
        @Test
        void returns200_whenRequestIsValid() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/likes")
                    .header("X-Loopers-LoginId", "testuser")
                    .header("X-Loopers-LoginPw", "Test1234!")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"productId": 1}
                    """))
                .andExpect(status().isOk());
        }
    }
}
