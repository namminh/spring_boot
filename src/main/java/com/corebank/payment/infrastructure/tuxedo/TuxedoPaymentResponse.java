package com.corebank.payment.infrastructure.tuxedo;

public record TuxedoPaymentResponse(boolean success, String responseCode, String responseMessage) {

    public static TuxedoPaymentResponse success(String responseCode) {
        return new TuxedoPaymentResponse(true, responseCode, "OK");
    }

    public static TuxedoPaymentResponse failure(String responseCode, String responseMessage) {
        return new TuxedoPaymentResponse(false, responseCode, responseMessage);
    }
}
