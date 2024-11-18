package com.itm.space.backendresources.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.service.UserService;
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


/**
 *  Интеграционные тесты для контроллера UserController, которые проверяют различные действия API.
 *  Эти тесты имитируют работу контроллера в контексте Spring, проверяют взаимодействие
 *  с сервисами, безопасность и корректность возврата данных.
 *
 *  @WebMvcTest(UserController.class): - Поднимает только контекст Spring MVC для тестирования контроллера.
 *              Это значит, что мы тестируем только веб-слой приложения.
 *
 *  @MockMvc: Для имитации HTTP-запросов к контроллеру, позволяя проверять HTTP-ответы.
 *
 *  @MockBean UserService: Мокируем зависимость от UserService, чтобы изолировать
 *              тестирование контроллера и не зависеть от реализации сервиса.
 */
@WebMvcTest(UserController.class)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;



    /**
     * Проверяем создание пользователя
     */
    @Nested
    class Create {

        // по хорошему - нужно разнести на несколько тестов, но в учебных целях - минимизируем код
        /**
         * Проверяет, что авторизованный пользователь с ролью MODERATOR может создать нового пользователя.
         * Используется CSRF-токен, так как это POST-запрос.
         */
        @Test
        @WithMockUser(roles = {"MODERATOR"}) // Поскольку контроллер защищён (исп-ся @Secured), нам нужно авторизовать пользователя, чтобы успешно выполнить тесты => Мы добавили эту аннотацию к тестам, чтобы проверять, что методы доступны только пользователям с нужными ролями.
        void shouldCreateUserSuccessfully_WhenPassedCorrectParameters() throws Exception {
            // Создаем объект запроса для создания пользователя
            UserRequest userRequest = new UserRequest(
                    "username_TestUser", // String username
                    "email_test@example.com", // String email
                    "password_", // String password
                    "firstName_", // String firstName
                    "lastName_"); // String lastName

            // Выполняем POST запрос к /api/users с корректными данными и CSRF токеном
            mockMvc.perform(post("/api/users")
                            .with(SecurityMockMvcRequestPostProcessors.csrf()) // Добавляем CSRF токен для безопасности
                            .contentType(MediaType.APPLICATION_JSON) // Указываем тип контента как JSON
                            .content(objectMapper.writeValueAsString(userRequest))) // Передаем тело запроса
                    .andExpect(status().isOk()); // Ожидаем статус ответа 200 OK, если все успешно

            // Проверяем, что сервисный метод createUser был вызван ровно один раз с нужными параметрами
            verify(userService, times(1))
                    .createUser(any(UserRequest.class));
        }

        /**
         * Проверяет валидацию данных при создании пользователя
         */
        @Test
        @WithMockUser(roles = {"MODERATOR"})
        void shouldReturnBadRequest_WhenUserRequestIsInvalid() throws Exception {
            UserRequest invalidUserRequest = new UserRequest(
                    "", // Некорректное имя пользователя (пустая строка)
                    "invalid_email", // Некорректный email
                    "123", // Некорректный пароль (слишком короткий)
                    "", // Пустое имя
                    "" // Пустая фамилия
            );

            mockMvc.perform(post("/api/users")
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidUserRequest)))
                    .andExpect(status().isBadRequest()); // Ожидаем статус 400 Bad Request
        }
    }



    /**
     * Проверяет получение информации о пользователе по его идентификатору
     */
    @Nested
    class GetUserById {

        /**
         * Проверяет, что информация о пользователе корректно возвращается,
         * если пользователь с ролью MODERATOR делает запрос.
         */
        @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnUserResponse_WhenCalledMethodGetUserById_AndUserWasSuccessfullyFound() throws Exception {
            // Создаем фиктивный ID пользователя
            final UUID userId = UUID.randomUUID();

            // Создаем ответ, который будет возвращен сервисом
            UserResponse userResponse = new UserResponse(
                    "firstName_", // Имя пользователя
                    "lastName_", // Фамилия пользователя
                    "email_test@example.com", // Email пользователя
                    List.of("ROLE_USER"), // Роли пользователя
                    List.of("TESTED_GROUP_A", "TESTED_GROUP_B") // Группы пользователя
            );

            // Мокаем поведение userService, чтобы вернуть UserResponse с предопределёнными значениями
            when(userService.getUserById(userId)).thenReturn(userResponse);

            // Выполняем GET-запрос и проверяем, что данные совпадают с ожидаемыми
            mockMvc.perform(get("/api/users/{id}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()) // Ожидаем статус ответа 200 OK
                    .andExpect(jsonPath("$.firstName").value("firstName_")) // Проверяем имя пользователя
                    .andExpect(jsonPath("$.lastName").value("lastName_")) // Проверяем фамилию пользователя
                    .andExpect(jsonPath("$.email").value("email_test@example.com")) // Проверяем email
                    .andExpect(jsonPath("$.roles[0]").value("ROLE_USER")) // Проверяем первую роль
                    .andExpect(jsonPath("$.groups[0]").value("TESTED_GROUP_A")) // Проверяем первую группу
                    .andExpect(jsonPath("$.groups[1]").value("TESTED_GROUP_B")); // Проверяем вторую группу
        }
    }



    /**
     * Проверяет метод hello, который возвращает имя текущего пользователя
     */
    @Nested
    class Hello {

        /**
         * Тест проверяет, что текущий авторизованный пользователь с именем testUser_ITM может получить свое имя.
         * Метод hello возвращает имя текущего пользователя.
         */
        @Test
        @WithMockUser(username = "testUser_ITM", roles = "MODERATOR")
        void shouldReturnReturnNameOfCurrentUse_WhenThisMethodWasCalled() throws Exception {
            // Выполняем GET-запрос к /api/users/hello и проверяем статус ответа и возвращаемое значение
            mockMvc.perform(get("/api/users/hello")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()) // Ожидаем статус ответа 200 OK
                    .andExpect(jsonPath("$").isString()); // Проверяем, что возвращаемое значение - имя пользователя "testUser_ITM"
        }

        /**
         * Тест проверяет, что если неавторизованный пользователь пытается сделать запрос
         * к защищенному ресурсу (в данном случае к /api/users), сервер отвечает 403 Forbidden.
         * @throws Exception ошибка
         */
        @Test
        void shouldReturnForbidden_WhenUnauthorized() throws Exception {
            // Выполняем POST-запрос к /api/users без авторизации
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")) // Пустое тело
                    .andExpect(status().isForbidden()); // Ожидаем статус ответа 403 Forbidden
        }
    }
}
