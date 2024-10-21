package sisyphus_core.sisyphus_core.auth.exception.exhandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserException;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserLoginIdException;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserNicknameException;
import sisyphus_core.sisyphus_core.auth.exception.InvalidAuthCodeException;

@ControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<String> handleDuplicateUserException(DuplicateUserException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(DuplicateUserLoginIdException.class)
    public ResponseEntity<String> handleDuplicateUserLoginIdException(DuplicateUserLoginIdException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(DuplicateUserNicknameException.class)
    public ResponseEntity<String> handleDuplicateUserNicknameException(DuplicateUserNicknameException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidAuthCodeException.class)
    public ResponseEntity<String> handleInvalidAuthCodeException(InvalidAuthCodeException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
