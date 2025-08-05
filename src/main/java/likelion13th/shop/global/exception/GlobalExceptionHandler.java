package likelion13th.shop.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import likelion13th.shop.global.api.ApiResponse;
import likelion13th.shop.global.api.ErrorCode;
import likelion13th.shop.global.api.ReasonDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

// 전역 예외 처리
@RestControllerAdvice(annotations = {RestController.class})
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    //ConstraintViolationException
    @ExceptionHandler
    public ResponseEntity<Object> validation(ConstraintViolationException e, WebRequest request) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("ConstraintViolationException 처리 중 에러 발생");
        return handleExceptionInternalConstraint(e, ErrorCode.BAD_REQUEST, HttpHeaders.EMPTY, request);
    }

    //GeneralException
    @ExceptionHandler(value = GeneralException.class)
    public ResponseEntity<Object> onThrowException(GeneralException generalException,
                                                   HttpServletRequest request) {
        ReasonDto reason = generalException.getReason();
        return handleExceptionInternal(generalException, reason, null, request);
    }

    // MethodArgumentNotValidException
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String fieldName = fieldError.getField();
            String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
            errors.put(fieldName, errorMessage);
        });
        return handleExceptionInternalArgs(e, HttpHeaders.EMPTY, ErrorCode.BAD_REQUEST, request, errors);
    }

    // Exception
    @ExceptionHandler
    public ResponseEntity<Object> exception(Exception e, WebRequest request) {
        e.printStackTrace();
        return handleExceptionInternalFalse(e, ErrorCode.INTERNAL_SERVER_ERROR, HttpHeaders.EMPTY,
                ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus(), request, e.getMessage());
    }

    // 공통 에러 응답
    private ResponseEntity<Object> handleExceptionInternal(Exception e, ReasonDto reason,
                                                           HttpHeaders headers, HttpServletRequest request) {
        ApiResponse<Object> body = ApiResponse.onFailure(reason, null);
        WebRequest webRequest = new ServletWebRequest(request);
        return super.handleExceptionInternal(e, body, headers, reason.getHttpStatus(), webRequest);
    }

    private ResponseEntity<Object> handleExceptionInternalFalse(Exception e, ErrorCode errorCode,
                                                                HttpHeaders headers, HttpStatus status, WebRequest request,
                                                                String errorPoint) {
        ApiResponse<Object> body = ApiResponse.onFailure(errorCode, errorPoint);
        return super.handleExceptionInternal(e, body, headers, status, request);
    }

    private ResponseEntity<Object> handleExceptionInternalArgs(Exception e, HttpHeaders headers,
                                                               ErrorCode errorCode, WebRequest request,
                                                               Map<String, String> errorArgs) {
        ApiResponse<Object> body = ApiResponse.onFailure(errorCode, errorArgs);
        return super.handleExceptionInternal(e, body, headers, errorCode.getHttpStatus(), request);
    }

    private ResponseEntity<Object> handleExceptionInternalConstraint(Exception e, ErrorCode errorCode,
                                                                     HttpHeaders headers, WebRequest request) {
        ApiResponse<Object> body = ApiResponse.onFailure(errorCode, null);
        return super.handleExceptionInternal(e, body, headers, errorCode.getHttpStatus(), request);
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<Object> handleFileUploadException(FileUploadException e) {
        System.out.println("📁 파일 업로드 예외: " + e.getMessage());

        ApiResponse<Object> body = ApiResponse.onFailure(e.getErrorCode(), null);
        return ResponseEntity.status(e.getErrorCode().getReason().getHttpStatus()).body(body);
    }
}
