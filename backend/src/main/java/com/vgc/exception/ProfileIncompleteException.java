package com.vgc.exception;

public class ProfileIncompleteException extends RuntimeException {
    public ProfileIncompleteException() {
        super("배송지(주소·휴대전화) 등록 후 응답할 수 있습니다.");
    }
}
