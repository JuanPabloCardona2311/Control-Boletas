package co.elmulardeoro.boletas.comun;

import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {

    public record Error(String mensaje) {}

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Error> api(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new Error(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Error> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new Error(mensaje));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Error> prohibido(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Error("Solo un administrador puede hacer esto"));
    }
}
