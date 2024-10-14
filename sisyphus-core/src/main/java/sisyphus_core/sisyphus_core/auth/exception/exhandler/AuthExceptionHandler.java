package sisyphus_core.sisyphus_core.auth.exception.exhandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserLoginIdException;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserNicknameException;

@ControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(DuplicateUserLoginIdException.class)
    public ResponseEntity<String> handleDuplicateUserLoginIdException(DuplicateUserLoginIdException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(DuplicateUserNicknameException.class)
    public ResponseEntity<String> handleDuplicateUserNicknameException(DuplicateUserNicknameException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
