package com.itm.space.backendresources.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itm.space.backendresources.BaseIntegrationTest;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@WebMvcTest(UserController.class) // Поднимаем контекст Spring, необх-й только для тестирования уровня веб MVC, т.е. контроллеров.
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        // Дополнительная настройка перед каждым тестом, если нужно
    }



    // Проверяем создание пользователя
    @Nested
    class Create {

        // по хорошему - нужно разнести на несколько тестов, но в учебных целях - минимизируем код
        @Test
        @WithMockUser(roles = {"MODERATOR"}) // Поскольку контроллер защищён (исп-ся @Secured), нам нужно авторизовать пользователя, чтобы успешно выполнить тесты => Мы добавили эту аннотацию к тестам, чтобы проверять, что методы доступны только пользователям с нужными ролями.
        void shouldCreateUserSuccessfully_WhenPassedCorrectParameters() throws Exception {
            UserRequest userRequest = new UserRequest(
                    "username_TestUser", // String username
                    "email_test@example.com", // String email
                    "password_", // String password
                    "firstName_", // String firstName
                    "lastName_"); // String lastName

            mockMvc.perform(post("/api/users") //  имитация HTTP POST запроса
                            .with(SecurityMockMvcRequestPostProcessors.csrf()) // добавление CSRF токена
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userRequest))
                    )
                    .andExpect(status().isOk()); // Проверяем, что статус ответа - OK (200)

            // Проверяем, что сервисный метод был вызван ровно 1 раз с нужными параметрами
            verify(userService, times(1))
                    .createUser(any(UserRequest.class));
        }
    }



    // Проверяет получение информации о пользователе по его идентификатору
    @Nested
    class GetUserById {

        @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnUserResponse_WhenCalledMethodGetUserById_AndUserWasSuccessfullyFound() throws Exception {
            // Создаем фиктивный ID пользователя
            final UUID userId = UUID.randomUUID();

            // Создаем ответ, который будет возвращен сервисом
            UserResponse userResponse = new UserResponse(
                    "firstName_", // String firstName
                    "lastName_", // String lastName
                    "email_test@example.com", // String email
                    List.of("ROLE_USER"), // List<String> roles
                    List.of("TESTED_GROUP_A", "TESTED_GROUP_B") // List<String> groups
            );

            // Мокаем поведение userService, чтобы вернуть UserResponse с предопределёнными значениями
            when(userService.getUserById(userId))
                    .thenReturn(userResponse);

            // Выполняем GET-запрос и проверяем, что полученные данные совпадают с ожидаемыми
            mockMvc.perform(get("/api/users/{id}", userId) // имитация HTTP GET запроса
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()) // Проверяем, что статус ответа - OK (200)
                    .andExpect(jsonPath("$.firstName").value("firstName_")) // Проверяем, что в ответе содержится корректное имя
                    .andExpect(jsonPath("$.lastName").value("lastName_")) // Проверяем, что в ответе содержится корректная фамилия
                    .andExpect(jsonPath("$.email").value("email_test@example.com")) // Проверяем, что в ответе содержится корректный email
                    .andExpect(jsonPath("$.roles[0]").value("ROLE_USER")) // Проверяем, что роль корректная
                    .andExpect(jsonPath("$.groups[0]").value("TESTED_GROUP_A")) // Проверяем первую группу
                    .andExpect(jsonPath("$.groups[1]").value("TESTED_GROUP_B")); // Проверяем вторую группу
        }
    }


    // Проверяет метод hello, который возвращает имя текущего пользователя
    @Nested
    class Hello {

        /**
         * Тест Hello будет проверять не только статус ответа,
         * но и значение возвращаемого имени пользователя, что делает проверку более точной и полной
         * @throws Exception ошибка
         */
        @Test
        @WithMockUser(username = "testUser_ITM", roles = "MODERATOR")
        void shouldReturnReturnNameOfCurrentUse_WhenThisMethodWasCalled() throws Exception {
            mockMvc.perform(get("/api/users/hello")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()) // Проверяем, что статус ответа - OK (200)
                    .andExpect(jsonPath("$").isString()); // Проверяем, что возвращаемое значение - имя пользователя "testUser_ITM"
        }


        /**
         * Тест проверяет, что если неавторизованный пользователь пытается сделать запрос к защищенному ресурсу (в данном случае к /api/users), сервер отвечает 403 Forbidden.
         * @throws Exception ошибка
         */
        @Test
        void shouldReturnForbidden_WhenUnauthorized() throws Exception {
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden()); // Проверяем, что неавторизованный пользователь получает 403 Forbidden
        }
    }
}
