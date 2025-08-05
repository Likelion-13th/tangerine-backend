package likelion13th.shop.global.exception;

import likelion13th.shop.global.api.BaseCode;

// FileUploadException.java
public class FileUploadException extends CustomException {
  public FileUploadException(BaseCode errorCode) {
    super(errorCode);
  }
}
