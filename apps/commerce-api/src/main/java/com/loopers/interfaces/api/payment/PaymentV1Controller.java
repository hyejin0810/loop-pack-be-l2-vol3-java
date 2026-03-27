package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentInfo;
import com.loopers.infrastructure.pg.PgCallbackPayload;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentV1Controller {

    private final PaymentFacade paymentFacade;

    @PostMapping
    public ApiResponse<PaymentV1Dto.PaymentResponse> requestPayment(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String rawPassword,
        @RequestBody PaymentV1Dto.PaymentRequest request
    ) {
        PaymentInfo info = paymentFacade.requestPayment(
            loginId, rawPassword, request.preOrderId(), request.cardType(), request.cardNo()
        );
        return ApiResponse.success(PaymentV1Dto.PaymentResponse.from(info));
    }

    /** PG 시스템이 결제 처리 완료 후 호출하는 콜백 */
    @PostMapping("/callback")
    public ApiResponse<Void> handleCallback(
        @RequestBody PgCallbackPayload payload
    ) {
        paymentFacade.handleCallback(payload);
        return ApiResponse.success(null);
    }

    /** 콜백 미수신 시 수동으로 PG 상태 동기화 */
    @PostMapping("/{paymentId}/sync")
    public ApiResponse<PaymentV1Dto.PaymentResponse> syncPaymentStatus(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String rawPassword,
        @PathVariable Long paymentId
    ) {
        PaymentInfo info = paymentFacade.syncPaymentStatus(loginId, rawPassword, paymentId);
        return ApiResponse.success(PaymentV1Dto.PaymentResponse.from(info));
    }
}
