package com.WEB4_5_GPT_BE.unihub.domain.course.exception;

import com.WEB4_5_GPT_BE.unihub.global.exception.UnihubException;

/**
 * 파일 업로드에 실패했을 때 발생하는 예외입니다.
 */
public class FileUploadException extends UnihubException {
    public FileUploadException() {
        super("500", "파일 업로드에 실패하였습니다.");
    }
}
