package zerowaste.backend.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import zerowaste.backend.security.AppUserDetailsService;
import zerowaste.backend.security.JwtService;
import zerowaste.backend.webSocket.WebSocketTokenInterceptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketTokenInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AppUserDetailsService userDetailsService;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private WebSocketTokenInterceptor interceptor;

    @Test
    void testPreSend_ConnectCommand_InvalidToken_DoesNotSetUser() {
        // Arrange
        String invalidToken = "invalid.token";
        String authHeader = "Bearer " + invalidToken;
        String userEmail = "test@example.com";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", authHeader);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Mock invalid token behavior
        when(jwtService.extractSubject(invalidToken, false)).thenReturn(userEmail);
        when(jwtService.isTokenValid(invalidToken, userEmail, false)).thenReturn(false); // Invalid!

        // Act
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Assert
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(resultAccessor);
        assertNull(resultAccessor.getUser()); // User should NOT be set
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void testPreSend_ConnectCommand_NoAuthHeader_DoesNotSetUser() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        // No Authorization header added
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Assert
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(resultAccessor);
        assertNull(resultAccessor.getUser());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void testPreSend_ConnectCommand_MalformedAuthHeader_DoesNotSetUser() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Basic 12345"); // Not Bearer
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Assert
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(resultAccessor);
        assertNull(resultAccessor.getUser());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void testPreSend_NotConnectCommand_Ignored() {
        // Arrange
        // Using SEND command instead of CONNECT
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.addNativeHeader("Authorization", "Bearer token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Assert
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(resultAccessor);
        assertNull(resultAccessor.getUser()); // Should be ignored
        verifyNoInteractions(jwtService);
    }

}
