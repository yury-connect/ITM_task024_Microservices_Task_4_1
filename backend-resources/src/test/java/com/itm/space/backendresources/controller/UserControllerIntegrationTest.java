package com.itm.space.backendresources.controller;

import com.itm.space.backendresources.BaseIntegrationTest;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


/**
 *  Интеграционные тесты для контроллера UserController, которые проверяют различные действия API.
 *  Эти тесты имитируют работу контроллера в контексте Spring, проверяют взаимодействие
 *  с сервисами, безопасность и корректность возврата данных.
 */
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private UserService userService;


    /**
     * Проверяем создание пользователя
     * (по хорошему - нужно разнести на несколько тестов, но в учебных целях - минимизируем код)
     */
    @Nested
    class CreateTests {

        /**
         * Проверяет, что пользователь с ролью MODERATOR может успешно создать нового пользователя
         * при передаче корректных данных, если пользователь еще не существует.
         */
        @Test
        @WithMockUser(roles = {"MODERATOR"}) // Поскольку контроллер защищён (исп-ся @Secured), нам нужно авторизовать пользователя, чтобы успешно выполнить тесты => Мы добавили эту аннотацию к тестам, чтобы проверять, что методы доступны только пользователям с нужными ролями.
        void shouldCreateUser_WhenUserDoesNotExist_AndRequestIsValid() throws Exception {
            // Создаем объект запроса для создания пользователя
            UserRequest userRequest = new UserRequest(
                    "username_TestUser", // String username
                    "email_test@example.com", // String email
                    "password_", // String password
                    "firstName_", // String firstName
                    "lastName_"); // String lastName

            // Выполняем POST запрос к /api/users с корректными данными
            mvc.perform(requestWithContent(post("/api/users"), userRequest))
                    .andExpect(status().isOk()); // Ожидаем статус ответа 200 OK, если все успешно

            // Проверяем, что сервисный метод createUser был вызван ровно один раз с нужными параметрами
            verify(userService, times(1))
                    .createUser(any(UserRequest.class));
        }

        /**
         * Проверяет, что попытка создания пользователя с уже существующим именем возвращает статус 409 Conflict.
         */
        @Test
        @WithMockUser(roles = {"MODERATOR"}) // Поскольку контроллер защищён, мы авторизуем пользователя с ролью MODERATOR
        void shouldReturnConflict_WhenCreatingUserThatAlreadyExists() throws Exception {
            // Создаем объект запроса для создания пользователя
            UserRequest userRequest = new UserRequest(
                    "username_ExistingUser", // String username
                    "existing_user@example.com", // String email
                    "password_", // String password
                    "firstName_", // String firstName
                    "lastName_"); // String lastName

            // Предполагаем, что такой пользователь уже существует в базе данных
            doThrow(new BackendResourcesException("User already exists", HttpStatus.CONFLICT))
                    .when(userService).createUser(any(UserRequest.class));

            // Выполняем POST запрос к /api/users с данными, которые уже существуют в базе данных
            mvc.perform(requestWithContent(post("/api/users"), userRequest))
                    .andExpect(status().isConflict()) // Ожидаем статус ответа 409 Conflict
                    .andDo(print()); // Выводим детали для отладки, если тест не прошел

            // Проверяем, что сервисный метод createUser был вызван ровно один раз с нужными параметрами
            verify(userService, times(1))
                    .createUser(any(UserRequest.class));
        }


        /**
         * Проверяет, что при передаче некорректных данных для создания пользователя
         * возвращается статус 400 Bad Request и соответствующие сообщения об ошибках.
         */
        @Test
        @WithMockUser(roles = {"MODERATOR"})
        void shouldReturnBadRequest_WithDetailedValidationErrors_WhenRequestDataIsInvalid() throws Exception {
            UserRequest invalidUserRequest = new UserRequest(
                    "", // Некорректное имя пользователя (пустая строка)
                    "invalid_email", // Некорректный email
                    "123", // Некорректный пароль (слишком короткий)
                    "", // Пустое имя
                    "" // Пустая фамилия
            );

            mvc.perform(requestWithContent(post("/api/users"), invalidUserRequest))
                    .andExpect(status().isBadRequest()) // Ожидаем статус 400 Bad Request
                    .andExpect(jsonPath("$.username")
                            .value("Username should not be blank"))
//                            .value("Username should be between 2 and 30 characters long"))
                    .andExpect(jsonPath("$.email")
                            .value("Email should be valid"))
                    .andExpect(jsonPath("$.password")
                            .value("Password should be greater than 4 characters long"))
                    .andExpect(jsonPath("$.firstName")
                            .value("must not be blank"))
                    .andExpect(jsonPath("$.lastName")
                            .value("must not be blank"));
        }
    }



    /**
     * Проверяет получение информации о пользователе по его идентификатору
     */
    @Nested
    class GetUserByIdTests {

        /**
         * Проверяет успешное получение информации о пользователе по его ID.
         */
        @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnUserResponse_WhenUserExists() throws Exception {
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
            mvc.perform(get("/api/users/{id}", userId)
                            .contentType(MediaType.APPLICATION_JSON)) // Указываем тип контента JSON
                    .andExpect(status().isOk()) // Ожидаем статус 200 OK
                    .andExpect(jsonPath("$.firstName")
                            .value("firstName_")) // Проверяем имя пользователя
                    .andExpect(jsonPath("$.lastName")
                            .value("lastName_")) // Проверяем фамилию пользователя
                    .andExpect(jsonPath("$.email")
                            .value("email_test@example.com")) // Проверяем email
                    .andExpect(jsonPath("$.roles[0]")
                            .value("ROLE_USER")) // Проверяем первую роль
                    .andExpect(jsonPath("$.groups[0]")
                            .value("TESTED_GROUP_A")) // Проверяем первую группу
                    .andExpect(jsonPath("$.groups[1]")
                            .value("TESTED_GROUP_B")); // Проверяем вторую группу
        }


        /**
         * Проверяет, что при запросе информации о несуществующем пользователе возвращается статус 404 Not Found.
         */
        @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
            // Создаем фиктивный ID пользователя, который не существует
            final UUID userId = UUID.randomUUID();

            // Мокаем поведение userService, чтобы выбрасывать исключение, если пользователь не найден
            when(userService.getUserById(userId)).thenThrow(new BackendResourcesException("User not found", HttpStatus.NOT_FOUND));

            // Выполняем GET-запрос и проверяем, что возвращается статус 404 Not Found
            mvc.perform(get("/api/users/{id}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }


        /**
         * Проверяет, что неавторизованный пользователь (без роли MODERATOR) не может получить информацию о пользователе.
         */
        @Test
        @WithMockUser(roles = "USER") // Пользователь с ролью USER, а не MODERATOR
        void shouldReturnForbidden_WhenUserHasInsufficientRole() throws Exception {
            // Создаем фиктивный ID пользователя
            final UUID userId = UUID.randomUUID();

            // Выполняем GET-запрос и ожидаем статус 403 Forbidden
            mvc.perform(get("/api/users/{id}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }


        /**
         * Проверяет, что при передаче некорректного ID возвращается статус 400 Bad Request.
         */
        @Test
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnBadRequest_WhenUserIdIsInvalid() throws Exception {
            // Передаем некорректный ID, например, строка, не являющаяся UUID
            String invalidUserId = "invalid-uuid";

            // Выполняем GET-запрос и ожидаем статус 400 Bad Request
            mvc.perform(get("/api/users/{id}", invalidUserId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }



    /**
     * Проверяет метод hello, который возвращает имя текущего пользователя
     */
    @Nested
    class HelloTests {

        /**
         * Проверяет, что текущий авторизованный пользователь с именем testUser_ITM может получить свое имя.
         */
        @Test
        @WithMockUser(username = "testUser_ITM", roles = "MODERATOR")
        void shouldReturnUsername_WhenAuthorizedUser_CallsHelloEndpoint() throws Exception {
            // Выполняем GET-запрос к /api/users/hello и проверяем статус ответа и возвращаемое значение
            mvc.perform(get("/api/users/hello")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()) // Ожидаем статус ответа 200 OK
                    .andExpect(jsonPath("$").isString()); // Проверяем, что возвращаемое значение - имя пользователя "testUser_ITM"
        }


        /**
         * Проверяет, что неавторизованный пользователь не может получить доступ к /api/users/hello и получает статус 401 Unauthorized.
         */
        @Test
        void shouldReturnUnauthorized_WhenUnauthenticatedUser_CallsHelloEndpoint() throws Exception {
            // Выполняем GET-запрос без авторизации
            mvc.perform(get("/api/users/hello")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized()); // Ожидаем статус ответа 401 Unauthorized
        }


        /**
         * Проверяет, что авторизованный пользователь без роли MODERATOR
         * не может получить доступ к /api/users/hello и получает статус 403 Forbidden.
         */
        @Test
        @WithMockUser(username = "testUser_ITM", roles = "USER")
        void shouldReturnForbidden_WhenUserWithoutModeratorRole_CallsHelloEndpoint() throws Exception {
            // Выполняем GET-запрос к /api/users/hello с пользователем, не имеющим роли MODERATOR
            mvc.perform(get("/api/users/hello")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden()); // Ожидаем статус ответа 403 Forbidden
        }


        /**
         * Проверяет, что текущий авторизованный пользователь с ролью MODERATOR
         * может получить свое имя через /api/users/hello.
         */
        @Test
        @WithMockUser(username = "moderatorUser_ITM", roles = "MODERATOR")
        void shouldReturnUsername_WhenModeratorUser_CallsHelloEndpoint() throws Exception {
            // Выполняем GET-запрос к /api/users/hello и проверяем статус ответа и возвращаемое значение
            mvc.perform(get("/api/users/hello")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()) // Ожидаем статус ответа 200 OK
                    .andExpect(jsonPath("$").value("moderatorUser_ITM")); // Проверяем, что возвращаемое значение - имя пользователя "moderatorUser_ITM"
        }
    }
}
