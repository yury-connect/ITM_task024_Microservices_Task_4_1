package com.itm.space.backendresources.service;

import com.itm.space.backendresources.BaseIntegrationTest;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@SpringBootTest
@AutoConfigureMockMvc
class UserServiceImplTest extends BaseIntegrationTest {

    @MockBean
    private Keycloak keycloakClient;

    @MockBean
    private UserMapper userMapper;

    @SpyBean
    private UserServiceImpl userService;

    @Value("${keycloak.realm}")
    private String realm;

    private RealmResource realmResource;
    private UsersResource usersResource;

    @BeforeEach
    void setUp() {
        realmResource = mock(RealmResource.class);
        usersResource = mock(UsersResource.class);

        when(keycloakClient.realm(realm)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
    }

    @Nested
    class CreateUser {

//        @Test
//        void createUser_Success() {
//            UserRequest userRequest = new UserRequest(
//                    "username_TestUser",
//                    "email_test@example.com",
//                    "password_",
//                    "firstName_",
//                    "lastName_"
//            );
//
//            // Мокируем Response
//            Response response = Mockito.mock(Response.class);
//
//            // Настраиваем статус ответа на CREATED (201)
//            when(response.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
//
//            // Мокируем статусную информацию, чтобы избежать NPE
//            Response.StatusType statusType = mock(Response.StatusType.class);
//            when(statusType.getFamily()).thenReturn(Response.Status.Family.SUCCESSFUL);
//            when(response.getStatusInfo()).thenReturn(statusType);
//
//            // Мокируем URI локации, как будто пользователь был создан успешно
//            when(response.getLocation()).thenReturn(URI.create("http://example.com/users/1"));
//
//            // Настраиваем поведение usersResource.create для возврата мокированного ответа
//            when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
//
//            // Проверяем, что метод createUser выполняется без выброса исключений
//            assertDoesNotThrow(() -> userService.createUser(userRequest));
//        }



        @Test
        void createUser_KeycloakException() {
            UserRequest userRequest = new UserRequest(
                    "username_TestUser",
                    "email_test@example.com",
                    "password_",
                    "firstName_",
                    "lastName_"
            );

            when(usersResource.create(any(UserRepresentation.class)))
                    .thenThrow(new WebApplicationException("Keycloak error", Response.Status.BAD_REQUEST));

            BackendResourcesException exception = assertThrows(BackendResourcesException.class, () -> userService.createUser(userRequest));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
            assertTrue(exception.getMessage().contains("Keycloak error"));
        }

//        @Test
//        void createUser_NullLocation() {
//            UserRequest userRequest = new UserRequest(
//                    "username_TestUser",
//                    "email_test@example.com",
//                    "password_",
//                    "firstName_",
//                    "lastName_"
//            );
//
//            // Мокируем Response
//            Response response = mock(Response.class);
//
//            // Настраиваем статус на CREATED (успешное создание)
//            when(response.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
//
//            // Настраиваем статусную информацию для успешного ответа
//            Response.StatusType statusType = mock(Response.StatusType.class);
//            when(statusType.getFamily()).thenReturn(Response.Status.Family.SUCCESSFUL);
//            when(response.getStatusInfo()).thenReturn(statusType);
//
//            // Мокируем URI локации, чтобы он был null (проверка обработки ошибки)
//            when(response.getLocation()).thenReturn(null);
//
//            // Настраиваем поведение usersResource.create для возврата мокированного ответа
//            when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
//
//            // Проверяем, что метод createUser выбрасывает BackendResourcesException при null location
//            BackendResourcesException exception = assertThrows(BackendResourcesException.class, () -> userService.createUser(userRequest));
//            assertTrue(exception.getMessage().contains("Location header is null, expected URI for created user"));
//        }
    }


    @Nested
    class GetUserById {

        @Test
        void getUserById_Success() {
            UUID userId = UUID.randomUUID();

            UserRepresentation userRepresentation = new UserRepresentation();
            userRepresentation.setId(userId.toString());
            userRepresentation.setUsername("testuser");

            List<RoleRepresentation> userRoles = List.of();
            List<GroupRepresentation> userGroups = List.of();

            var userResource = mock(UserResource.class);
            var roleMappingResource = mock(RoleMappingResource.class);
            var mappingsRepresentation = mock(MappingsRepresentation.class);

            // Настраиваем моки для получения пользователя, ролей и групп
            when(usersResource.get(userId.toString())).thenReturn(userResource);
            when(userResource.toRepresentation()).thenReturn(userRepresentation);
            when(userResource.roles()).thenReturn(roleMappingResource);
            when(roleMappingResource.getAll()).thenReturn(mappingsRepresentation);
            when(mappingsRepresentation.getRealmMappings()).thenReturn(userRoles);
            when(userResource.groups()).thenReturn(userGroups);

            when(userMapper.userRepresentationToUserResponse(userRepresentation, userRoles, userGroups))
                    .thenReturn(new UserResponse("firstName", "lastName", "email@example.com", List.of(), List.of()));

            // Act
            UserResponse userResponse = userService.getUserById(userId);

            // Assert
            assertNotNull(userResponse);
            assertEquals("firstName", userResponse.getFirstName());
            assertEquals("lastName", userResponse.getLastName());
            assertEquals("email@example.com", userResponse.getEmail());
        }

        @Test
        void getUserById_RuntimeException() {
            UUID userId = UUID.randomUUID();

            // Настраиваем моки для выброса RuntimeException при вызове usersResource.get()
            when(usersResource.get(userId.toString())).thenThrow(new RuntimeException("Неожиданная ошибка"));

            RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
                userService.getUserById(userId);
            });

            assertEquals("Неожиданная ошибка", thrownException.getMessage(), "Сообщение об ошибке не совпадает");
        }
    }
}
